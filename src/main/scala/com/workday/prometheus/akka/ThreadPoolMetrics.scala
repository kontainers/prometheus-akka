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
import java.util.concurrent.ThreadPoolExecutor

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.concurrent.TrieMap

import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.GaugeMetricFamily

object ThreadPoolMetrics extends Collector {
  val map = TrieMap[String, Option[ThreadPoolExecutor]]()
  this.register()
  override def collect(): java.util.List[MetricFamilySamples] = {
    val dispatcherNameList = List("dispatcherName").asJava
    val activeThreadCountGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_active_thread_count",
      "Akka ThreadPool Dispatcher Active Thread Count", dispatcherNameList)
    val corePoolSizeGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_core_pool_size",
      "Akka ThreadPool Dispatcher Core Pool Size", dispatcherNameList)
    val currentPoolSizeGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_current_pool_size",
      "Akka ThreadPool Dispatcher Current Pool Size", dispatcherNameList)
    val largestPoolSizeGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_largest_pool_size",
      "Akka ThreadPool Dispatcher Largest Pool Size", dispatcherNameList)
    val maxPoolSizeGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_max_pool_size",
      "Akka ThreadPool Dispatcher Max Pool Size", dispatcherNameList)
    val completedTaskCountGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_completed_task_count",
      "Akka ThreadPoolExecutor Dispatcher Completed Task Count", dispatcherNameList)
    val totalTaskCountGauge = new GaugeMetricFamily("akka_dispatcher_threadpoolexecutor_total_task_count",
      "Akka ThreadPoolExecutor Dispatcher Total Task Count", dispatcherNameList)
    map.foreach { case (dispatcherName, tpeOption) =>
      val dispatcherNameList = List(dispatcherName).asJava
      tpeOption match {
        case Some(tpe) => {
          activeThreadCountGauge.addMetric(dispatcherNameList, tpe.getActiveCount)
          corePoolSizeGauge.addMetric(dispatcherNameList, tpe.getCorePoolSize)
          currentPoolSizeGauge.addMetric(dispatcherNameList, tpe.getPoolSize)
          largestPoolSizeGauge.addMetric(dispatcherNameList, tpe.getLargestPoolSize)
          maxPoolSizeGauge.addMetric(dispatcherNameList, tpe.getMaximumPoolSize)
          completedTaskCountGauge.addMetric(dispatcherNameList, tpe.getCompletedTaskCount)
          totalTaskCountGauge.addMetric(dispatcherNameList, tpe.getTaskCount)
        }
        case None => {
          activeThreadCountGauge.addMetric(dispatcherNameList, 0)
          corePoolSizeGauge.addMetric(dispatcherNameList, 0)
          currentPoolSizeGauge.addMetric(dispatcherNameList, 0)
          largestPoolSizeGauge.addMetric(dispatcherNameList, 0)
          maxPoolSizeGauge.addMetric(dispatcherNameList, 0)
          completedTaskCountGauge.addMetric(dispatcherNameList, 0)
          totalTaskCountGauge.addMetric(dispatcherNameList, 0)
        }
      }
    }
    val jul = new java.util.ArrayList[MetricFamilySamples]
    jul.add(activeThreadCountGauge)
    jul.add(corePoolSizeGauge)
    jul.add(currentPoolSizeGauge)
    jul.add(largestPoolSizeGauge)
    jul.add(maxPoolSizeGauge)
    jul.add(completedTaskCountGauge)
    jul.add(totalTaskCountGauge)
    Collections.unmodifiableList(jul)
  }

  def add(dispatcherName: String, tpe: ThreadPoolExecutor): Unit = {
    map.put(dispatcherName, Some(tpe))
  }

  def remove(dispatcherName: String): Unit = {
    map.put(dispatcherName, None)
  }
}
