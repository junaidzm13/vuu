package org.finos.vuu.core.module

import org.finos.vuu.api.{JoinTableDef, NoViewPortDef, TableDef, ViewPortDef}
import org.finos.vuu.core.VuuServer
import org.finos.vuu.core.table.{DataTable, TableContainer}
import org.finos.vuu.net.rest.RestService
import org.finos.vuu.net.rpc.RpcHandler
import org.finos.vuu.provider.{NullProvider, Provider, ProviderContainer}
import org.finos.toolbox.lifecycle.LifecycleContainer
import org.finos.toolbox.time.Clock

import java.nio.file.Path


case class TableDefs protected(realizedTableDefs: List[TableDef], tableDefs: List[(TableDef, (DataTable, VuuServer) => Provider)], joinDefs: List[TableDefContainer => JoinTableDef]) {

  def add(tableDef: TableDef, func: (DataTable, VuuServer) => Provider): TableDefs = {
    TableDefs(realizedTableDefs, tableDefs ++ List((tableDef, func)), joinDefs)
  }

  def addRealized(tableDef: TableDef): TableDefs = {
    TableDefs(realizedTableDefs ++ List(tableDef), tableDefs, joinDefs)
  }

  def addJoin(func: TableDefContainer => JoinTableDef): TableDefs = {
    TableDefs(realizedTableDefs, tableDefs, joinDefs ++ List(func))
  }

  protected def getJoinDefFuncs(): List[TableDefContainer => JoinTableDef] = joinDefs

  protected def getTableDefsAndProviders(): List[(TableDef, (DataTable, VuuServer) => Provider)] = tableDefs

  protected def getRealizedTableDefs(): List[TableDef] = realizedTableDefs

  def get(name: String): TableDef = {
    realizedTableDefs.find(_.name == name) match {
      case Some(tableDef) =>
        tableDef
      case None =>
        throw new Exception(s"Table $name could not be found in [" + realizedTableDefs.map(_.name).mkString(",") + "]")
    }
  }
}

case class ModuleFactoryNode protected(tableDefs: TableDefs,
                                       rpc: List[VuuServer => RpcHandler],
                                       vsName: String, staticServedResources: List[StaticServedResource],
                                       rest: List[VuuServer => RestService],
                                       viewPortDefs: Map[String, (DataTable, Provider, ProviderContainer, TableContainer) => ViewPortDef],
                                       tableDefContainer: TableDefContainer = new TableDefContainer(Map())) {

  def addTable(tableDef: TableDef, func: (DataTable, VuuServer) => Provider): ModuleFactoryNode = {
    val noViewPortDefFunc = (dt: DataTable, prov: Provider, providerContainer: ProviderContainer, tableContainer: TableContainer) => NoViewPortDef
    ModuleFactoryNode(tableDefs.add(tableDef, func), rpc, vsName, staticServedResources, rest, viewPortDefs ++ Map(tableDef.name -> noViewPortDefFunc), tableDefContainer)
  }

  def addTable(tableDef: TableDef, func: (DataTable, VuuServer) => Provider, func2: (DataTable, Provider, ProviderContainer, TableContainer) => ViewPortDef): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs.add(tableDef, func), rpc, vsName, staticServedResources, rest, viewPortDefs ++ Map(tableDef.name -> func2), tableDefContainer)
  }

  def addSessionTable(tableDef: TableDef, func2: (DataTable, Provider, ProviderContainer, TableContainer) => ViewPortDef): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs.add(tableDef, (dt, vs) => NullProvider), rpc, vsName, staticServedResources, rest, viewPortDefs ++ Map(tableDef.name -> func2), tableDefContainer)
  }

  def addSessionTable(tableDef: TableDef): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs.add(tableDef, (dt, vs) => NullProvider), rpc, vsName, staticServedResources, rest, viewPortDefs, tableDefContainer)
  }

  def addJoinTable(func: TableDefContainer => JoinTableDef): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs.addJoin(func), rpc, vsName, staticServedResources, rest, viewPortDefs, tableDefContainer)
  }

  def addRpcHandler(rpcFunc: VuuServer => RpcHandler): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs, rpc ++ List(rpcFunc), vsName, staticServedResources, rest, viewPortDefs, tableDefContainer)
  }

  def addRestService(restFunc: VuuServer => RestService): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs, rpc, vsName, staticServedResources, rest ++ List(restFunc), viewPortDefs, tableDefContainer)
  }

  /**
   * Add a statically served path to the view server.
   *
   * @param uriDirectory example /foo/bar/ (which will end up being served as https://your-server/<MODULE_NAME>/foo/bar
   * @param path         path to the directory on the machine you'd like to serve
   * @param canBrowse    can users browse the contents of the directory (listings)
   */
  def addStaticResource(uriDirectory: String, path: Path, canBrowse: Boolean): ModuleFactoryNode = {
    ModuleFactoryNode(tableDefs, rpc, vsName, staticServedResources ++ List(StaticServedResource(uriDirectory, path, canBrowse)), rest, viewPortDefs, tableDefContainer)
  }

  def asModule(): ViewServerModule = {

    val baseTables = tableDefs.tableDefs
    val justBaseTables = baseTables.map({ case (tbl, provFunc) => tbl })

    var mutableTableDefs = TableDefs(justBaseTables, List(), List())

    tableDefContainer.add(this.vsName, mutableTableDefs)

    //we do this loop to allow join tables to depend on other realized join tables
    tableDefs.joinDefs.foreach(toJTFunc => {
      val realizedJoinTableDef = toJTFunc(tableDefContainer)
      mutableTableDefs = mutableTableDefs.addRealized(realizedJoinTableDef)
      tableDefContainer.add(this.vsName, mutableTableDefs)
    })

    val theName = vsName
    val parentRef = this
    val tableConDef = tableDefContainer

    new ViewServerModule {
      override def name: String = theName

      override def tableDefs: List[TableDef] = mutableTableDefs.realizedTableDefs

      override def tableDefContainer: TableDefContainer = tableConDef

      override def serializationMixin: AnyRef = null

      override def rpcHandlersUnrealized: List[VuuServer => RpcHandler] = {
        rpc
      }

      override def getProviderForTable(table: DataTable, viewserver: VuuServer)(implicit time: Clock, lifecycleContainer: LifecycleContainer): Provider = {
        baseTables.find({ case (td, func) => td.name == table.name }).get._2(table, viewserver)
      }

      override def staticFileResources(): List[StaticServedResource] = staticServedResources

      override def restServicesUnrealized: List[VuuServer => RestService] = rest

      override def viewPortDefs: Map[String, (DataTable, Provider, ProviderContainer, TableContainer) => ViewPortDef] = parentRef.viewPortDefs
    }

  }
}

object ModuleFactory {

  private val tableDefContainer = new TableDefContainer(Map())

  implicit def stringToString(s: String): FieldDefString = new FieldDefString(s)

  def withNamespace(ns: String): ModuleFactoryNode = {
    ModuleFactoryNode(TableDefs(List(), List(), List()), List(), ns, List(), List(), Map(), tableDefContainer)
  }

}
