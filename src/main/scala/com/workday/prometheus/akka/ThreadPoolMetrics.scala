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

import java.util.concurrent.ThreadPoolExecutor

import scala.collection.JavaConverters._

import com.workday.prometheus.akka.impl.DoubleFunction

import io.micrometer.core.instrument.{ImmutableTag, Tag}

object ThreadPoolMetrics {

  val DispatcherName = "dispatcherName"

  def add(dispatcherName: String, tpe: ThreadPoolExecutor): Unit = {
    import AkkaMetricRegistry._
    val tags: Iterable[Tag] = Seq(new ImmutableTag(DispatcherName, dispatcherName))
    val jtags = tags.asJava
    val activeCountFn = new DoubleFunction[ThreadPoolExecutor](_.getActiveCount)
    val corePoolSizeFn = new DoubleFunction[ThreadPoolExecutor](_.getCorePoolSize)
    val poolSizeFn = new DoubleFunction[ThreadPoolExecutor](_.getPoolSize)
    val largestPoolSizeFn = new DoubleFunction[ThreadPoolExecutor](_.getLargestPoolSize)
    val maximumPoolSizeFn = new DoubleFunction[ThreadPoolExecutor](_.getMaximumPoolSize)
    val completedCountFn = new DoubleFunction[ThreadPoolExecutor](_.getCompletedTaskCount)
    val taskCountFn = new DoubleFunction[ThreadPoolExecutor](_.getTaskCount)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_active_thread_count", jtags, tpe, activeCountFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_core_pool_size", jtags, tpe, corePoolSizeFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_current_pool_size", jtags, tpe, poolSizeFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_largest_pool_size", jtags, tpe, largestPoolSizeFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_max_pool_size", jtags, tpe, maximumPoolSizeFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_completed_task_count", jtags, tpe, completedCountFn)
    getRegistry.gauge("akka_dispatcher_threadpoolexecutor_total_task_count", jtags, tpe, taskCountFn)
  }
}