/*
 * =========================================================================================
 * Copyright © 2017 Workday, Inc.
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */
package com.workday.prometheus.akka

import akka.actor._
import akka.monitor.instrumentation.CellInfo
import akka.routing._
import akka.testkit.TestProbe

class RouterMetricsSpec extends TestKitBaseSpec("RouterMetricsSpec") {

  import RouterMetricsTestActor._

  "the router metrics" should {
    "respect the configured include and exclude filters" in {
      val trackedRouter = createTestPoolRouter("tracked-pool-router")
      val nonTrackedRouter = createTestPoolRouter("non-tracked-pool-router")
      val excludedTrackedRouter = createTestPoolRouter("tracked-explicitly-excluded-pool-router")

      routerMetricsRecorderOf(trackedRouter) should not be empty
      routerMetricsRecorderOf(nonTrackedRouter) shouldBe empty
      routerMetricsRecorderOf(excludedTrackedRouter) shouldBe empty

      val metrics = routerMetricsRecorderOf(trackedRouter).get
      metrics.actorName shouldEqual "routermetricsspec_user_tracked_pool_router"
      metrics.messages.count() shouldEqual 1.0
    }
  }

  def routerMetricsRecorderOf(ref: ActorRef): Option[RouterMetrics] = {
    val name = CellInfo.cellName(system, ref)
    val entity = Entity(name, MetricsConfig.Router)
    if (RouterMetrics.hasMetricsFor(entity)) {
      Some(RouterMetrics.metricsFor(entity))
    } else {
      None
    }
  }

  def createTestPoolRouter(routerName: String): ActorRef = {
    val router = system.actorOf(RoundRobinPool(5).props(Props[RouterMetricsTestActor]), routerName)
    val initialiseListener = TestProbe()

    // Ensure that the router has been created before returning.
    router.tell(Ping, initialiseListener.ref)
    initialiseListener.expectMsg(Pong)

    router
  }
}
