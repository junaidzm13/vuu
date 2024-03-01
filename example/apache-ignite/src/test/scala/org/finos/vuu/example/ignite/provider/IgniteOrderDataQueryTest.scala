package org.finos.vuu.example.ignite.provider

import org.finos.toolbox.collection.array.ImmutableArray
import org.finos.toolbox.jmx.MetricsProviderImpl
import org.finos.toolbox.time.{Clock, TestFriendlyClock}
import org.finos.vuu.api.SessionTableDef
import org.finos.vuu.core.module.ModuleFactory.stringToString
import org.finos.vuu.core.sort.SortDirection
import org.finos.vuu.core.table.{Column, Columns, EmptyRowData, EmptyTablePrimaryKeys, RowWithData, TableData}
import org.finos.vuu.example.ignite.provider.IgniteOrderDataQueryTest.{entitySchema, internalColumns, internalColumnsByExternalFields}
import org.finos.vuu.example.ignite.store.IgniteStore
import org.finos.vuu.feature.inmem.InMemTablePrimaryKeys
import org.finos.vuu.net.{ClientSessionId, FilterSpec}
import org.finos.vuu.plugin.virtualized.api.VirtualizedSessionTableDef
import org.finos.vuu.plugin.virtualized.table.VirtualizedSessionTable
import org.finos.vuu.test.TestFriendlyJoinTableProvider
import org.finos.vuu.util.schema.{ExternalEntitySchema, ExternalEntitySchemaBuilder, SchemaMapper}
import org.scalamock.scalatest.MockFactory
import org.scalatest.featurespec.AnyFeatureSpec

class IgniteOrderDataQueryTest extends AnyFeatureSpec with MockFactory {
  private implicit val clock: Clock = new TestFriendlyClock(1_000)
  private implicit val metricsProvider: MetricsProviderImpl = new MetricsProviderImpl()

  private val store: IgniteStore[TestDto] = stub[IgniteStore[TestDto]]
  private val schemaMapper = SchemaMapper(entitySchema, internalColumns, internalColumnsByExternalFields)
  private val igniteDataQuery = IgniteOrderDataQuery(store, schemaMapper)

  Feature("fetch") {
    Scenario("can parse and apply filter and sort spec") {
      (store.findEntities _).when(*, *, *, *).returns(Iterator.empty)

      val filterSpec = FilterSpec("id = 2 and name != \"ABC\"")
      val sortSpec = Map("value" -> SortDirection.Ascending)

      igniteDataQuery.fetch(FetchParams(filterSpec, sortSpec, 0, 0))
      (store.findEntities _).verify("(key = 2 AND name != 'ABC')", "value ASC", *, *).once()
    }
  }

  Feature("fetchAndUpdateTable") {
    val fetchedEntities = List(TestDto(1, "name1", 10), TestDto(2, "name2", 20), TestDto(3, "name3", 30))
    val internalRows = fetchedEntities.map(schemaMapper.toInternalRowMap)

    Scenario("GIVEN table is empty WHEN fetch and update table THEN updates table with all fetched entities") {
      (store.findEntities _).when(*, *, *, *).returns(fetchedEntities.iterator)
      val mockTable = getMockTable
      (mockTable.primaryKeys _).when().returns(EmptyTablePrimaryKeys)
      (mockTable.pullRow(_: String)).when(*).returns(EmptyRowData)

      igniteDataQuery.fetchAndUpdateTable(
        mockTable,
        FetchParams(FilterSpec(""), Map.empty, rowCount = 3, startIndex = 0)
      )

      (mockTable.processUpdateForIndex _).verify(0,"1", RowWithData("1", internalRows.head), *).once()
      (mockTable.processUpdateForIndex _).verify(1,"2", RowWithData("2", internalRows(1)), *).once()
      (mockTable.processUpdateForIndex _).verify(2,"3", RowWithData("3", internalRows(2)), *).once()
    }

    Scenario("GIVEN data changes for one of the row " +
      "WHEN fetch & update THEN update table only at index where data has changed") {
      (store.findEntities _).when(*, *, *, *).returns(fetchedEntities.iterator)
      val mockTable = getMockTable
      val keys = InMemTablePrimaryKeys(ImmutableArray.fromArray(Array("1", "2", "3")))
      (mockTable.primaryKeys _).when().returns(keys)

      val changedDataFor1 = schemaMapper.toInternalRowMap(fetchedEntities.head.copy(name = "new-name1"))
      (mockTable.pullRow(_: String)).when("1").returns(RowWithData("1", changedDataFor1))
      (mockTable.pullRow(_: String)).when("2").returns(RowWithData("2", internalRows(1)))
      (mockTable.pullRow(_: String)).when("3").returns(RowWithData("3", internalRows(2)))

      igniteDataQuery.fetchAndUpdateTable(
        mockTable,
        FetchParams(FilterSpec(""), Map.empty, rowCount = 3, startIndex = 0)
      )

      (mockTable.processUpdateForIndex _).verify(0, "1", RowWithData("1", internalRows.head), *).once()
      (mockTable.processUpdateForIndex _).verify(1, *, *, *).never()
      (mockTable.processUpdateForIndex _).verify(2, *, *, *).never()
    }

    Scenario("GIVEN keys change at 2 indexes but data remains the same " +
      "WHEN fetch & update THEN update at all indexes where keys changed") {
      (store.findEntities _).when(*, *, *, *).returns(fetchedEntities.iterator)
      val mockTable = getMockTable
      val keys = InMemTablePrimaryKeys(ImmutableArray.fromArray(Array("1", "3", "2")))
      (mockTable.primaryKeys _).when().returns(keys)

      (mockTable.pullRow(_: String)).when("1").returns(RowWithData("1", internalRows.head))
      (mockTable.pullRow(_: String)).when("3").returns(RowWithData("2", internalRows(1)))
      (mockTable.pullRow(_: String)).when("2").returns(RowWithData("3", internalRows(2)))

      igniteDataQuery.fetchAndUpdateTable(
        mockTable,
        FetchParams(FilterSpec(""), Map.empty, rowCount = 3, startIndex = 0)
      )

      (mockTable.processUpdateForIndex _).verify(0, *, *, *).never()
      (mockTable.processUpdateForIndex _).verify(1, "2", RowWithData("2", internalRows(1)), *).once()
      (mockTable.processUpdateForIndex _).verify(2, "3", RowWithData("3", internalRows(2)), *).once()
    }

    def getMockTable: VirtualizedSessionTable = {
      val mockTable = stub[MockVirtualizedSessionTable]
      (mockTable.getTableDef _).when().returns(tableDef)
      mockTable
    }
  }

  class MockVirtualizedSessionTable extends VirtualizedSessionTable(ClientSessionId("test-sessionId", "test-user"), tableDef, new TestFriendlyJoinTableProvider()) {
    final override def name: String = super.name
    final override def plusName(s: String): String = super.plusName(s)
    final override def createDataTableData(): TableData = super.createDataTableData()
  }

  def tableDef: SessionTableDef = {
    VirtualizedSessionTableDef("testTable", internalColumnsByExternalFields("key"), internalColumns)
  }
}

private case class TestDto(key: Int, name: String, value: Int)

private object IgniteOrderDataQueryTest {
  val internalColumns: Array[Column] = Columns.fromNames("id".int(), "name".string(), "value".string())

  val internalColumnsByExternalFields: Map[String, String] = Map(
    "key" -> "id",
    "name" -> "name",
    "value" -> "value",
  )

  val entitySchema: ExternalEntitySchema = ExternalEntitySchemaBuilder().withCaseClass[TestDto].build()
}