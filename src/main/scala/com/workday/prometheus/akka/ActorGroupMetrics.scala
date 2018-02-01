/*
 * =========================================================================================
 * Copyright © 2017, 2018 Workday, Inc.
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

object ActorGroupMetrics {

  val GroupName = "groupName"

  private[akka] val MailboxMetricName = "akka_actor_group_mailboxes_size"
  private[akka] val ProcessingTimeMetricName = "akka_actor_group_processing_time"
  private[akka] val TimeInMailboxMetricName = "akka_actor_group_time_in_mailboxes"
  private[akka] val MessageCountMetricName = "akka_actor_group_message_count"
  private[akka] val ActorCountMetricName = "akka_actor_group_actor_count"
  private[akka] val ErrorCountMetricName = "akka_actor_group_error_count"

  import AkkaMetricRegistry._

  def mailboxSize(group: String) = gauge(MailboxMetricName, tagSeq(group))
  def processingTime(group: String) = timer(ProcessingTimeMetricName, tagSeq(group))
  def timeInMailbox(group: String) = timer(TimeInMailboxMetricName, tagSeq(group))
  def messages(group: String) = counter(MessageCountMetricName, tagSeq(group))
  def actorCount(group: String) = gauge(ActorCountMetricName, tagSeq(group))
  def errors(group: String) = counter(ErrorCountMetricName, tagSeq(group))
  private def tagSeq(group: String): Iterable[Tag] = Seq(new ImmutableTag(GroupName, group))
}
