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

import scala.concurrent.duration._

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually

import com.workday.prometheus.akka.ActorSystemMetrics._

import akka.actor.Props
import io.micrometer.core.instrument.ImmutableTag

class ActorSystemMetricsSpec extends TestKitBaseSpec("ActorSystemMetricsSpec") with BeforeAndAfterEach with Eventually {

  "the actor system metrics" should {
    "count actors" in {
      val originalMetrics = findSystemMetricsRecorder(system.name)
      val originalCount = originalMetrics.getOrElse(ActorCountMetricName, 0.0)
      val trackedActor = system.actorOf(Props[ActorMetricsTestActor])
      eventually(timeout(5 seconds)) {
        val map = findSystemMetricsRecorder(system.name)
        map should not be empty
        map.getOrElse(ActorCountMetricName, -1.0) shouldEqual (originalCount + 1.0)
      }
      system.stop(trackedActor)
      eventually(timeout(5 seconds)) {
        val metrics = findSystemMetricsRecorder(system.name)
        metrics.getOrElse(ActorCountMetricName, -1.0) shouldEqual originalCount
      }
    }
    "count unhandled messages" in {
      val count = findSystemMetricsRecorder(system.name).getOrElse(UnhandledMessageCountMetricName, 0.0)
      val trackedActor = system.actorOf(Props[ActorMetricsTestActor])
      trackedActor ! "unhandled"
      eventually(timeout(5 seconds)) {
        findSystemMetricsRecorder(system.name).getOrElse(UnhandledMessageCountMetricName, -1.0) shouldEqual (count + 1.0)
      }
    }
    "count dead letters" in {
      val count = findSystemMetricsRecorder(system.name).getOrElse(DeadLetterCountMetricName, 0.0)
      val trackedActor = system.actorOf(Props[ActorMetricsTestActor])
      system.stop(trackedActor)
      eventually(timeout(5 seconds)) {
        trackedActor ! "dead"
        findSystemMetricsRecorder(system.name).getOrElse(DeadLetterCountMetricName, -1.0) shouldBe > (count)
      }
    }
  }

  def findSystemMetricsRecorder(name: String): Map[String, Double] = {
    AkkaMetricRegistry.metricsForTags(Seq(new ImmutableTag(ActorSystemMetrics.ActorSystem, name)))
  }
}
