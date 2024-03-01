package org.finos.vuu.example.ignite.provider

import com.typesafe.scalalogging.StrictLogging
import org.finos.toolbox.time.Clock
import org.finos.vuu.example.ignite.module.IgniteOrderDataModule
import org.finos.vuu.example.ignite.provider.IgniteOrderDataProvider.columnNameByExternalField
import org.finos.vuu.example.ignite.query.IndexCalculator
import org.finos.vuu.example.ignite.schema.ChildOrderEntityObject
import org.finos.vuu.example.ignite.store.{IgniteOrderStore, IgniteStore}
import org.finos.vuu.plugin.virtualized.table.{VirtualizedRange, VirtualizedSessionTable, VirtualizedViewPortKeys}
import org.finos.vuu.provider.VirtualizedProvider
import org.finos.vuu.util.schema.SchemaMapper
import org.finos.vuu.viewport.ViewPort

class IgniteOrderDataProvider(final val igniteStore: IgniteOrderStore)
                             (implicit clock: Clock) extends VirtualizedProvider with StrictLogging {

  private val schemaMapper = SchemaMapper(ChildOrderEntityObject.getSchema, IgniteOrderDataModule.columns, columnNameByExternalField)
  private val dataQuery = IgniteOrderDataQuery(igniteStore.asInstanceOf[IgniteStore[Product]], schemaMapper)
  private val indexCalculator = IndexCalculator(extraRowsCount = 5000)

  override def runOnce(viewPort: ViewPort): Unit = {

    val internalTable = viewPort.table.asTable.asInstanceOf[VirtualizedSessionTable]

    val igniteFilter =  dataQuery.getFilterSql(viewPort.filterSpec)
    val totalSize: Int = getTotalSize(igniteFilter).toInt

    val viewPortRange = viewPort.getRange
    logger.debug(s"Calculating index for view port range ${viewPortRange.from} and ${viewPortRange.to} for total rows of $totalSize")
    val (startIndex, endIndex, rowCount) = indexCalculator.calc(viewPortRange, totalSize)

    internalTable.setSize(totalSize)//todo should this be long?
    internalTable.setRange(VirtualizedRange(startIndex, endIndex))

    logger.info(s"Loading data between $startIndex and $endIndex for $rowCount rows where total size $totalSize")

    dataQuery.fetchAndUpdateTable(internalTable, FetchParams(
      viewPort.filterSpec,
      viewPort.sortSpecInternal,
      startIndex = startIndex,
      rowCount = rowCount)
    )

    viewPort.setKeys(new VirtualizedViewPortKeys(internalTable.primaryKeys))
  }

  private def getTotalSize(filter: String): Long = igniteStore.getCount(filter)

  override def subscribe(key: String): Unit = {}

  override def doStart(): Unit = {}

  override def doStop(): Unit = {}

  override def doInitialize(): Unit = {}

  override def doDestroy(): Unit = {}

  override val lifecycleId: String = "org.finos.vuu.example.ignite.provider.IgniteOrderDataProvider"

  override def getUniqueValues(columnName: String): Array[String] =
    igniteStore.getDistinct(columnName, 10).toArray

  override def getUniqueValuesStartingWith(columnName: String, starts: String): Array[String] =
    igniteStore.getDistinct(columnName, starts, 10).toArray

}

object IgniteOrderDataProvider {
  val columnNameByExternalField: Map[String, String] = Map(
    "id" -> "orderId",
    "ric" -> "ric",
    "price" -> "price",
    "quantity" -> "quantity",
    "side" -> "side",
    "strategy" -> "strategy",
    "parentId" -> "parentId",
  )
}
