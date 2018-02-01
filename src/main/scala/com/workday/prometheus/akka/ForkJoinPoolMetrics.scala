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

import java.util.Collections

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.concurrent.TrieMap

import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.GaugeMetricFamily

object ForkJoinPoolMetrics extends Collector {
  val map = TrieMap[String, Option[ForkJoinPoolLike]]()
  this.register()
  override def collect(): java.util.List[MetricFamilySamples] = {
    val dispatcherNameList = List("dispatcherName").asJava
    val parallelismGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_parellelism",
      "Akka ForkJoinPool Dispatcher Parellelism", dispatcherNameList)
    val poolSizeGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_pool_size",
      "Akka ForkJoinPool Dispatcher Pool Size", dispatcherNameList)
    val activeThreadCountGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_active_thread_count",
      "Akka ForkJoinPool Dispatcher Active Thread Count", dispatcherNameList)
    val runningThreadCountGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_running_thread_count",
      "Akka ForkJoinPool Dispatcher Running Thread Count", dispatcherNameList)
    val queuedTaskCountGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_queued_task_count",
      "Akka ForkJoinPool Dispatcher Queued Task Count", dispatcherNameList)
    val queuedSubmissionCountGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_queued_submission_count",
      "Akka ForkJoinPool Dispatcher Queued Submission Count", dispatcherNameList)
    val stealCountGauge = new GaugeMetricFamily("akka_dispatcher_forkjoinpool_steal_count",
      "Akka ForkJoinPool Dispatcher Steal Count", dispatcherNameList)
    map.foreach { case (dispatcherName, fjpOption) =>
      val dispatcherNameList = List(dispatcherName).asJava
      fjpOption match {
        case Some(fjp) => {
          parallelismGauge.addMetric(dispatcherNameList, fjp.getParallelism)
          poolSizeGauge.addMetric(dispatcherNameList, fjp.getPoolSize)
          activeThreadCountGauge.addMetric(dispatcherNameList, fjp.getActiveThreadCount)
          runningThreadCountGauge.addMetric(dispatcherNameList, fjp.getRunningThreadCount)
          queuedSubmissionCountGauge.addMetric(dispatcherNameList, fjp.getQueuedSubmissionCount)
          queuedTaskCountGauge.addMetric(dispatcherNameList, fjp.getQueuedTaskCount)
          stealCountGauge.addMetric(dispatcherNameList, fjp.getStealCount)
        }
        case None => {
          parallelismGauge.addMetric(dispatcherNameList, 0)
          poolSizeGauge.addMetric(dispatcherNameList, 0)
          activeThreadCountGauge.addMetric(dispatcherNameList, 0)
          runningThreadCountGauge.addMetric(dispatcherNameList, 0)
          queuedSubmissionCountGauge.addMetric(dispatcherNameList, 0)
          queuedTaskCountGauge.addMetric(dispatcherNameList, 0)
          stealCountGauge.addMetric(dispatcherNameList, 0)
        }
      }

    }
    val jul = new java.util.ArrayList[MetricFamilySamples]
    jul.add(parallelismGauge)
    jul.add(poolSizeGauge)
    jul.add(activeThreadCountGauge)
    jul.add(runningThreadCountGauge)
    jul.add(queuedSubmissionCountGauge)
    jul.add(queuedTaskCountGauge)
    jul.add(stealCountGauge)
    Collections.unmodifiableList(jul)
  }

  def add(dispatcherName: String, fjp: ForkJoinPoolLike): Unit = {
    map.put(dispatcherName, Some(fjp))
  }

  def remove(dispatcherName: String): Unit = {
    map.put(dispatcherName, None)
  }
}
