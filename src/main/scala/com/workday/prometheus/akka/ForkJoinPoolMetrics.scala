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

import scala.collection.JavaConverters._

import com.workday.prometheus.akka.impl.DoubleFunction

import io.micrometer.core.instrument.{ImmutableTag, Tag}

object ForkJoinPoolMetrics {
  val DispatcherName = "dispatcherName"

  def add(dispatcherName: String, fjp: ForkJoinPoolLike): Unit = {
    import AkkaMetricRegistry._
    val tags: Iterable[Tag] = Seq(new ImmutableTag(DispatcherName, dispatcherName))
    val jtags = tags.asJava
    val parellelismFn = new DoubleFunction[ForkJoinPoolLike](_.getParallelism)
    val poolSizeFn = new DoubleFunction[ForkJoinPoolLike](_.getParallelism)
    val activeThreadCountFn = new DoubleFunction[ForkJoinPoolLike](_.getActiveThreadCount)
    val runningThreadCountFn = new DoubleFunction[ForkJoinPoolLike](_.getRunningThreadCount)
    val queuedSubmissionCountFn = new DoubleFunction[ForkJoinPoolLike](_.getQueuedSubmissionCount)
    val queuedTaskCountFn = new DoubleFunction[ForkJoinPoolLike](_.getQueuedTaskCount)
    val stealCountFn = new DoubleFunction[ForkJoinPoolLike](_.getStealCount)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_parellelism", jtags, fjp, parellelismFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_pool_size", jtags, fjp, poolSizeFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_active_thread_count", jtags, fjp, activeThreadCountFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_running_thread_count", jtags, fjp, runningThreadCountFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_queued_task_count", jtags, fjp, queuedSubmissionCountFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_queued_submission_count", jtags, fjp, queuedTaskCountFn)
    getRegistry.gauge("akka_dispatcher_forkjoinpool_steal_count", jtags, fjp, stealCountFn)
  }
}
