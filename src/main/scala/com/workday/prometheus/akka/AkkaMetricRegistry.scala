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
  private var timerRegistryMap = TrieMap[MeterRegistry, TrieMap[MeterKey, Timer]]()

  def getRegistry: MeterRegistry = registry.getOrElse(simpleRegistry)

  def setRegistry(registry: MeterRegistry): Unit = {
    this.registry = Option(registry)
  }

  def counter(name: String, tags: Iterable[Tag]): Counter = {
    def javaTags = tags.asJava
    counterMap.getOrElseUpdate(MeterKey(name, tags), getRegistry.counter(name, javaTags))
  }

  def timer(name: String, tags: Iterable[Tag]): Timer = {
    def javaTags = tags.asJava
    timerMap.getOrElseUpdate(MeterKey(name, tags), getRegistry.timer(name, javaTags))
  }

  private def counterMap: TrieMap[MeterKey, Counter] = {
    counterRegistryMap.getOrElseUpdate(getRegistry, { TrieMap[MeterKey, Counter]() })
  }

  private def timerMap: TrieMap[MeterKey, Timer] = {
    timerRegistryMap.getOrElseUpdate(getRegistry, { TrieMap[MeterKey, Timer]() })
  }
}
