/*
 * =========================================================================================
 * Copyright © 2017,2018 Workday, Inc.
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
package io.kontainers.micrometer.akka

import scala.concurrent.Future

import akka.actor._
import akka.dispatch.MessageDispatcher
import akka.testkit.TestProbe
import io.kontainers.micrometer.akka.ThreadPoolMetrics.DispatcherName
import io.micrometer.core.instrument.ImmutableTag

object DispatcherMetricsSpec {
  val SystemName = "DispatcherMetricsSpec"

  def findDispatcherRecorder(dispatcherName: String): Map[String, Double] = {
    val tags = Seq(new ImmutableTag(DispatcherName, dispatcherName))
    AkkaMetricRegistry.metricsForTags(tags)
  }
}

class DispatcherMetricsSpec extends TestKitBaseSpec(DispatcherMetricsSpec.SystemName) {

  override def beforeAll(): Unit = {
    super.beforeAll()
    AkkaMetricRegistry.clear()
  }

  "the akka dispatcher metrics" should {
    "respect the configured include and exclude filters" in {
      forceInit(system.dispatchers.lookup("akka.actor.default-dispatcher"))
      val fjpDispatcher = forceInit(system.dispatchers.lookup("tracked-fjp"))
      val tpeDispatcher = forceInit(system.dispatchers.lookup("tracked-tpe"))
      val excludedDispatcher = forceInit(system.dispatchers.lookup("explicitly-excluded"))

      import DispatcherMetricsSpec.findDispatcherRecorder
      findDispatcherRecorder(fjpDispatcher.id) shouldNot be(empty)
      findDispatcherRecorder(tpeDispatcher.id) shouldNot be(empty)
      findDispatcherRecorder(excludedDispatcher.id) should be(empty)
    }
  }

  def forceInit(dispatcher: MessageDispatcher): MessageDispatcher = {
    val listener = TestProbe()
    Future {
      listener.ref ! "init done"
    }(dispatcher)
    listener.expectMsg("init done")

    dispatcher
  }
}
