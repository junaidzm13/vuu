//package org.finos.vuu.example.ignite.provider
//
//import org.finos.toolbox.jmx.{MetricsProvider, MetricsProviderImpl}
//import org.finos.toolbox.time.{Clock, TestFriendlyClock}
//import org.finos.vuu.api.{Indices, SessionTableDef}
//import org.finos.vuu.core.table.TableData
//import org.finos.vuu.example.ignite.store.IgniteOrderStore
//import org.finos.vuu.net.ClientSessionId
//import org.finos.vuu.plugin.virtualized.table.VirtualizedSessionTable
//import org.finos.vuu.test.TestFriendlyJoinTableProvider
//import org.finos.vuu.viewport.{RowSource, ViewPort}
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.featurespec.AnyFeatureSpec
//import org.scalatest.matchers.should.Matchers
//
//class IgniteOrderDataProviderTest extends AnyFeatureSpec with Matchers with MockFactory {
//  private implicit val clock: Clock = new TestFriendlyClock(10001)
//  private implicit val metricsProvider: MetricsProvider = new MetricsProviderImpl()
//  private val joinProvider = new TestFriendlyJoinTableProvider()
//  private val virtualizedSessionTable = stub[MockVirtualizedSessionTable]
//
//  private val rowSource = stub[RowSource]
//  (rowSource.asTable _).when().returns(virtualizedSessionTable)
//  private val viewPort = stub[ViewPort]
//  (viewPort.table _).when().returns(rowSource)
//
//
//  private val igniteStore = stub[IgniteOrderStore]
//  (igniteStore.findChildOrder _).when(*, *, *, *).returns(Iterator.empty)
//
//  private val provider = new IgniteOrderDataProvider(igniteStore)
//
//  Feature("") {
//    Scenario("") {
//
//    }
//  }
//
//  class MockVirtualizedSessionTable extends VirtualizedSessionTable(
//    ClientSessionId("test-sessionId", "test-user"),
//    new SessionTableDef("test-table", "id", Array.empty, Seq.empty, indices = Indices()),
//    joinProvider
//  ) {
//    final override def name: String = ""
//    final override def plusName(s: String): String = super.plusName(s)
//    final override def createDataTableData(): TableData = super.createDataTableData()
//  }
//}
