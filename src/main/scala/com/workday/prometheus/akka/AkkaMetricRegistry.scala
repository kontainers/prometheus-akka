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
    def javaTags = tags.asJava
    TimerWrapper(timerMap.getOrElseUpdate(MeterKey(name, tags), getRegistry.timer(name, javaTags)))
  }

  private[akka] def clear(): Unit = {
    timerRegistryMap.clear()
    gaugeRegistryMap.clear()
    counterRegistryMap.clear()
  }

  private[akka] def metricsForTags(tags: Seq[Tag]): Map[String, Double] = {
    val filtered: Iterable[(String, Double)] = getRegistry.getMeters.asScala.flatMap { meter =>
      val id = meter.getId
      if (id.getTags.asScala == tags) {
        meter.measure().asScala.headOption.map { measure =>
          (id.getName, measure.getValue)
        }
      } else {
        None
      }
    }
    filtered.toMap
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
