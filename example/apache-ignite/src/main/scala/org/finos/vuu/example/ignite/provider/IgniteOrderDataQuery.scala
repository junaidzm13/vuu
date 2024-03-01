package org.finos.vuu.example.ignite.provider

import org.finos.toolbox.time.Clock
import org.finos.vuu.core.sort.ModelType.SortSpecInternal
import org.finos.vuu.core.table.RowWithData
import org.finos.vuu.example.ignite.store.IgniteStore
import org.finos.vuu.feature.ignite.FilterAndSortSpecToSql
import org.finos.vuu.net.FilterSpec
import org.finos.vuu.plugin.virtualized.table.VirtualizedSessionTable
import org.finos.vuu.util.schema.SchemaMapper

class IgniteOrderDataQuery [T <: Product] private (private val igniteStore: IgniteStore[T],
                                                   private val schemaMapper: SchemaMapper)(implicit val clock: Clock) {

  private val filterAndSortSpecToSql = FilterAndSortSpecToSql(schemaMapper)

  def getFilterSql(filterSpec: FilterSpec): String =
    filterAndSortSpecToSql.filterToSql(filterSpec)

  def fetch(params: FetchParams): Iterator[Map[String, Any]] = {
    igniteStore.findEntities(
      filterAndSortSpecToSql.filterToSql(params.filterSpec),
      filterAndSortSpecToSql.sortToSql(params.sortSpecInternal),
      startIndex = params.startIndex,
      rowCount = params.rowCount
    ).map(schemaMapper.toInternalRowMap)
  }

  def fetchAndUpdateTable(table: VirtualizedSessionTable, fetchParams: FetchParams): Unit = {
    val startIndex = fetchParams.startIndex
    val keyField = table.getTableDef.keyField
    def hasRowChangedAtIndex = getHasRowChanged(table)

    fetch(fetchParams)
      .map(toRowWithData(keyField, _))
      .zipWithIndex
      .filter({ case (row, i) => hasRowChangedAtIndex(startIndex + i, row) })
      .foreach({ case (row, i) => table.processUpdateForIndex(startIndex + i, row.key, row, clock.now()) })
  }

  private def toRowWithData(keyField: String, data: Map[String, Any]) = RowWithData(data(keyField).toString, data)

  private def getHasRowChanged(table: VirtualizedSessionTable) = (index: Int, newRow: RowWithData) => {
    val existingKeyAtIndex = table.primaryKeys.get(index)
    existingKeyAtIndex != newRow.key || !table.pullRow(existingKeyAtIndex).equals(newRow)
  }

}

object IgniteOrderDataQuery {
  def apply[T <: Product](igniteStore: IgniteStore[T], schemaMapper: SchemaMapper)(implicit clock: Clock): IgniteOrderDataQuery[T] =
    new IgniteOrderDataQuery(igniteStore, schemaMapper)
}

case class FetchParams(filterSpec: FilterSpec, sortSpecInternal: SortSpecInternal, startIndex: Int, rowCount: Int)
