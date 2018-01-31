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

import scala.collection.JavaConverters._

import io.prometheus.client._

object ActorMetrics {
  private val map: concurrent.Map[Entity, ActorMetrics] = new java.util.concurrent.ConcurrentHashMap[Entity, ActorMetrics]().asScala
  def metricsFor(e: Entity) = map.getOrElseUpdate(e, new ActorMetrics(e))
  def hasMetricsFor(e: Entity) = map.contains(e)
}

class ActorMetrics(entity: Entity) {
  val actorName = metricFriendlyActorName(entity.name)
  val mailboxSize = Gauge.build().name(s"akka_actor_mailbox_size_$actorName").help("Akka Actor mailbox size").register()
  val processingTime = Gauge.build().name(s"akka_actor_processing_time_$actorName").help("Akka Actor processing time (Seconds)").register()
  val timeInMailbox = Gauge.build().name(s"akka_actor_time_in_mailbox_$actorName").help("Akka Actor time in mailbox (Seconds)").register()
  val messages = Counter.build().name(s"akka_actor_message_count_$actorName").help("Akka Actor messages").register()
  val errors = Counter.build().name(s"akka_actor_error_count_$actorName").help("Akka Actor errors").register()
}
