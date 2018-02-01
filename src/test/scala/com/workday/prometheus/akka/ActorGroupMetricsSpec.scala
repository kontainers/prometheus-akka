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

import akka.actor._
import akka.routing.RoundRobinPool
import akka.testkit.TestProbe
import io.micrometer.core.instrument.ImmutableTag

class ActorGroupMetricsSpec extends TestKitBaseSpec("ActorGroupMetricsSpec") with BeforeAndAfterEach with Eventually {

  import ActorGroupMetrics._

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearGroupMetrics
  }

  "the actor group metrics" should {
    "respect the configured include and exclude filters" in {
      val trackedActor = createTestActor("tracked-actor")
      val nonTrackedActor = createTestActor("non-tracked-actor")
      val excludedTrackedActor = createTestActor("tracked-explicitly-excluded-actor")

      findGroupRecorder("tracked") should not be empty
      findGroupRecorder("exclusive") shouldBe empty
      val map = findGroupRecorder("tracked")
      map.getOrElse(ActorCountMetricName, -1.0) shouldEqual 1.0
      map.getOrElse(MessageCountMetricName, -1.0) shouldEqual 1.0
      map.getOrElse(MailboxMetricName, -1.0) shouldEqual 0.0

      system.stop(trackedActor)
      eventually(timeout(5 seconds)) {
        val metrics = findGroupRecorder("tracked")
        metrics.getOrElse(ActorCountMetricName, -1.0) shouldEqual 0.0
        metrics.getOrElse(ProcessingTimeMetricName, -1.0) should (be >= 0.0)
        metrics.getOrElse(ProcessingTimeMetricName, -1.0) should (be <= 1.0)
        metrics.getOrElse(TimeInMailboxMetricName, -1.0) should (be >= 0.0)
        metrics.getOrElse(TimeInMailboxMetricName, -1.0) should (be <= 1.0)
      }

      val trackedActor2 = createTestActor("tracked-actor2")
      val trackedActor3 = createTestActor("tracked-actor3")

      val map2 = findGroupRecorder("tracked")
      map2.getOrElse(ActorCountMetricName, -1.0) shouldEqual 2.0
      map2.getOrElse(MessageCountMetricName, -1.0) should (be >= 3.0)
    }

    "respect the configured include and exclude filters for routee actors" in {
      val trackedRouter = createTestPoolRouter("tracked-router")
      val nonTrackedRouter = createTestPoolRouter("non-tracked-router")
      val excludedTrackedRouter = createTestPoolRouter("tracked-explicitly-excluded-router")

      findGroupRecorder("tracked") should not be empty
      findGroupRecorder("exclusive") shouldBe empty
      val map = findGroupRecorder("tracked")
      map.getOrElse(ActorCountMetricName, -1.0) shouldEqual 5.0
      map.getOrElse(MessageCountMetricName, -1.0) shouldEqual 1.0
      map.getOrElse(MailboxMetricName, -1.0) shouldEqual 0.0

      system.stop(trackedRouter)
      eventually(timeout(5 seconds)) {
        findGroupRecorder("tracked").getOrElse(ActorCountMetricName, -1.0) shouldEqual 0.0
      }

      val trackedRouter2 = createTestPoolRouter("tracked-router2")
      val trackedRouter3 = createTestPoolRouter("tracked-router3")
      findGroupRecorder("tracked").getOrElse(ActorCountMetricName, -1.0) shouldEqual 10.0

      val map2 = findGroupRecorder("tracked")
      map2.getOrElse(ActorCountMetricName, -1.0) shouldEqual 10.0
      map2.getOrElse(MessageCountMetricName, -1.0) shouldEqual 3.0
    }
  }

  def findGroupRecorder(groupName: String): Map[String, Double] = {
    AkkaMetricRegistry.metricsForTags(Seq(new ImmutableTag(ActorGroupMetrics.GroupName, groupName)))
  }

  def clearGroupMetrics: Unit = {
    AkkaMetricRegistry.clear()
  }

  def createTestActor(name: String): ActorRef = {
    val actor = system.actorOf(Props[ActorMetricsTestActor], name)
    val initialiseListener = TestProbe()

    // Ensure that the router has been created before returning.
    import ActorMetricsTestActor._
    actor.tell(Ping, initialiseListener.ref)
    initialiseListener.expectMsg(Pong)

    actor
  }

  def createTestPoolRouter(routerName: String): ActorRef = {
    val router = system.actorOf(RoundRobinPool(5).props(Props[RouterMetricsTestActor]), routerName)
    val initialiseListener = TestProbe()

    // Ensure that the router has been created before returning.
    import RouterMetricsTestActor._
    router.tell(Ping, initialiseListener.ref)
    initialiseListener.expectMsg(Pong)

    router
  }

}
