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
package com.workday.prometheus.akka

class MetricsConfigSpec extends BaseSpec {
  "MetricsConfig" should {
    "contain the expected group names" in {
      MetricsConfig.groupNames should contain allOf ("all", "tracked", "empty", "exclusive")
    }
    "track correct actor groups" in {
      MetricsConfig.actorShouldBeTrackedUnderGroups("system1/hello/MyActor1") should contain theSameElementsAs List("all", "exclusive")
      MetricsConfig.actorShouldBeTrackedUnderGroups("system1/hello/NotMyActor1") should contain theSameElementsAs List("all")
    }
    "track correct actors" in {
      MetricsConfig.shouldTrack(MetricsConfig.Actor, "system1/user/tracked-actor1") shouldBe true
      MetricsConfig.shouldTrack(MetricsConfig.Actor, "system1/user/non-tracked-actor1") shouldBe false
    }
    "track correct routers" in {
      MetricsConfig.shouldTrack(MetricsConfig.Router, "system1/user/tracked-pool-router") shouldBe true
      MetricsConfig.shouldTrack(MetricsConfig.Router, "system1/user/non-tracked-pool-router") shouldBe false
    }
    "track correct dispatchers" in {
      MetricsConfig.shouldTrack(MetricsConfig.Dispatcher, "system1/hello/MyDispatcher1") shouldBe true
      MetricsConfig.shouldTrack(MetricsConfig.Dispatcher, "system1/hello/explicitly-excluded") shouldBe false
    }
  }
}
