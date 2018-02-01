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

import io.micrometer.core.instrument.{ImmutableTag, Tag}

object ActorSystemMetrics {

  val ActorSystem = "actorSystem"

  private[akka] val ActorCountMetricName = "akka_system_actor_count"
  private[akka] val DeadLetterCountMetricName = "akka_system_dead_letter_count"
  private[akka] val UnhandledMessageCountMetricName = "akka_system_unhandled_message_count"

  import AkkaMetricRegistry._

  def actorCount(system: String) = gauge(ActorCountMetricName, tagSeq(system))
  def deadLetterCount(system: String) = counter(DeadLetterCountMetricName, tagSeq(system))
  def unhandledMessageCount(system: String) = counter(UnhandledMessageCountMetricName, tagSeq(system))
  private def tagSeq(system: String): Iterable[Tag] = Seq(new ImmutableTag(ActorSystem, system))
}
