package io.venuu.vuu.viewport

import io.venuu.vuu.core.table.TableTestHelper.{combineQs, emptyQueues}
import io.venuu.vuu.util.table.TableAsserts.assertVpEq
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.Tables.Table

/**
 * When we update a viewport range, moving from say 0-20 to 10-30, we only want to send updates for the additional rows,
 * no the whole content of the viewport. In this example it would be rows 20-30.
 */
class OnlySendDiffRowsViewPortTest extends AbstractViewPortTestCase with Matchers with GivenWhenThen{

  Feature("Check when we update view port ranges, we only send the new rows"){

    Scenario("Change viewport from 0-20 to 10-30 and check we only get 10 rows"){

      val (viewPortContainer, orders, ordersProvider, session, outQueue, highPriorityQueue) = createDefaultViewPortInfra()

      val vpcolumns = List("orderId", "trader", "tradeTime", "quantity", "ric").map(orders.getTableDef.columnForName(_)).toList

      createNOrderRows(ordersProvider, 10)(timeProvider)

      val viewPort = viewPortContainer.create(session, outQueue, highPriorityQueue, orders, ViewPortRange(0, 4), vpcolumns)

      viewPortContainer.runOnce()

      val combinedUpdates = combineQs(viewPort)

      combinedUpdates(0).size should equal(10)

      assertVpEq(combinedUpdates){
        Table(
          ("orderId" ,"trader"  ,"ric"     ,"tradeTime","quantity"),
          ("NYC-0000","chris"   ,"VOD.L"   ,1311544800l,100       ),
          ("NYC-0001","chris"   ,"VOD.L"   ,1311544810l,101       ),
          ("NYC-0002","chris"   ,"VOD.L"   ,1311544820l,102       ),
          ("NYC-0003","chris"   ,"VOD.L"   ,1311544830l,103       )
        )
      }

      val viewPortv2 = viewPortContainer.changeRange(session, highPriorityQueue, viewPort.id, ViewPortRange(2, 6))

      assertVpEq(viewPortv2.highPriorityQ.popUpTo(20)){
        Table(
          //don't send 2,3 as they already existed in client cache
          ("orderId" ,"trader"  ,"ric"     ,"tradeTime","quantity"),
          ("NYC-0004","chris"   ,"VOD.L"   ,1311544840l,104       ),
          ("NYC-0005","chris"   ,"VOD.L"   ,1311544850l,105       )
        )
      }

      emptyQueues(viewPortv2)

      viewPortContainer.runOnce()

      assertVpEq(combineQs(viewPortv2)){
        Table(
          //don't send 2,3 as they already existed in client cache
          ("orderId" ,"trader"  ,"ric"     ,"tradeTime","quantity"),
        )
      }

    }

  }

}
