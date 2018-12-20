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
package io.kontainers.micrometer

import java.util.regex.Pattern

import scala.annotation.tailrec

package object akka {
  def metricFriendlyActorName(actorPath: String) = {
    sanitizeMetricName(trimLeadingSlashes(actorPath).toLowerCase.replace("/", "_"))
  }

  private val SANITIZE_PREFIX_PATTERN = Pattern.compile("^[^a-zA-Z_]")
  private val SANITIZE_BODY_PATTERN = Pattern.compile("[^a-zA-Z0-9_]")

  // borrowed from io.prometheus.client.Collector
  def sanitizeMetricName(metricName: String): String = {
    SANITIZE_BODY_PATTERN.matcher(SANITIZE_PREFIX_PATTERN.matcher(metricName).replaceFirst("_")).replaceAll("_")
  }

  @tailrec
  private def trimLeadingSlashes(s: String): String = {
    if (s.startsWith("/")) trimLeadingSlashes(s.substring(1)) else s
  }
}
