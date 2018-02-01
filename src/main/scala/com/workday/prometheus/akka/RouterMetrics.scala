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

import scala.collection.concurrent.TrieMap

object RouterMetrics {
  private val map = new TrieMap[Entity, RouterMetrics]()
  def metricsFor(e: Entity) = map.getOrElseUpdate(e, new RouterMetrics(e))
  def hasMetricsFor(e: Entity) = map.contains(e)
}

class RouterMetrics(entity: Entity) {
  import AkkaMetricRegistry._
  val actorName = metricFriendlyActorName(entity.name)
  val routingTime = timer(s"akka_router_routing_time_$actorName", Seq.empty)
  val processingTime = timer(s"akka_router_processing_time_$actorName", Seq.empty)
  val timeInMailbox = timer(s"akka_router_time_in_mailbox_$actorName", Seq.empty)
  val messages = counter(s"akka_router_message_count_$actorName", Seq.empty)
  val errors = counter(s"akka_router_error_count_$actorName", Seq.empty)
}
