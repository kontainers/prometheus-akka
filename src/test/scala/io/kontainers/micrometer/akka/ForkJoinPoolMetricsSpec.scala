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

import org.slf4j.LoggerFactory

class ForkJoinPoolMetricsSpec extends BaseSpec {

  val logger = LoggerFactory.getLogger(classOf[ForkJoinPoolMetricsSpec])

  override def beforeAll(): Unit = {
    super.beforeAll()
    AkkaMetricRegistry.clear()
  }

  "ForkJoinPoolMetrics" should {
    "support java forkjoinpool" in {
      val name = "ForkJoinPoolMetricsSpec-java-pool"
      val pool = new java.util.concurrent.ForkJoinPool
      try {
        ForkJoinPoolMetrics.add(name, pool.asInstanceOf[ForkJoinPoolLike])
        DispatcherMetricsSpec.findDispatcherRecorder(name) should not be(empty)
      } finally {
        pool.shutdownNow()
      }
    }

    "support scala forkjoinpool" in {
      try {
        val clazz = Class.forName("scala.concurrent.forkjoin.ForkJoinPool")
        val name = "ForkJoinPoolMetricsSpec-scala-pool"
        val pool = clazz.newInstance
        try {
          ForkJoinPoolMetrics.add(name, pool.asInstanceOf[ForkJoinPoolLike])
          DispatcherMetricsSpec.findDispatcherRecorder(name) should not be (empty)
        } finally {
          val method = clazz.getMethod("shutdownNow")
          method.invoke(pool)
        }
      } catch {
        case _: ClassNotFoundException => {
          logger.warn("skipping scala forkjoinpool test as class no longer supported")
        }
      }
    }
  }
}
