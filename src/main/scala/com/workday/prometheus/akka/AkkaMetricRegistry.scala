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

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap

import io.micrometer.core.instrument._
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

object AkkaMetricRegistry {
  private val simpleRegistry = new SimpleMeterRegistry
  private var registry: Option[MeterRegistry] = None
  private case class MeterKey(name: String, tags: Iterable[Tag])
  private var counterRegistryMap = TrieMap[MeterRegistry, TrieMap[MeterKey, Counter]]()
  private var gaugeRegistryMap = TrieMap[MeterRegistry, TrieMap[MeterKey, GaugeWrapper]]()
  private var timerRegistryMap = TrieMap[MeterRegistry, TrieMap[MeterKey, Timer]]()

  def getRegistry: MeterRegistry = registry.getOrElse(simpleRegistry)

  def setRegistry(registry: MeterRegistry): Unit = {
    this.registry = Option(registry)
  }

  def counter(name: String, tags: Iterable[Tag]): Counter = {
    def javaTags = tags.asJava
    counterMap.getOrElseUpdate(MeterKey(name, tags), getRegistry.counter(name, javaTags))
  }

  def gauge(name: String, tags: Iterable[Tag]): GaugeWrapper = {
    gaugeMap.getOrElseUpdate(MeterKey(name, tags), GaugeWrapper(getRegistry, name, tags))
  }

  def timer(name: String, tags: Iterable[Tag]): TimerWrapper = {
    def createTimer = {
      val builder = Timer.builder(name).tags(tags.asJava)
      if (MetricsConfig.histogramBucketsEnabled) {
        builder.publishPercentileHistogram()
      }
      builder.register(getRegistry)
    }
    TimerWrapper(timerMap.getOrElseUpdate(MeterKey(name, tags), createTimer))
  }

  private[akka] def clear(): Unit = {
    timerRegistryMap.clear()
    gaugeRegistryMap.clear()
    counterRegistryMap.clear()
  }

  private[akka] def metricsForTags(tags: Seq[Tag]): Map[String, Double] = {
    val tagsList = tags.toList
    val filtered: Iterable[(String, Double)] = getRegistry.getMeters.asScala.flatMap { meter =>
      val id = meter.getId
      if (id.getTags.asScala.toList == tagsList) {
        meter.measure().asScala.headOption.map { measure =>
          (id.getName, measure.getValue)
        }
      } else {
        None
      }
    }
    filtered.groupBy(_._1).map { case (key, list) =>
      (key, list.map(_._2).sum)
    }
  }

  private def counterMap: TrieMap[MeterKey, Counter] = {
    counterRegistryMap.getOrElseUpdate(getRegistry, { TrieMap[MeterKey, Counter]() })
  }

  private def gaugeMap: TrieMap[MeterKey, GaugeWrapper] = {
    gaugeRegistryMap.getOrElseUpdate(getRegistry, { TrieMap[MeterKey, GaugeWrapper]() })
  }

  private def timerMap: TrieMap[MeterKey, Timer] = {
    timerRegistryMap.getOrElseUpdate(getRegistry, { TrieMap[MeterKey, Timer]() })
  }
}
