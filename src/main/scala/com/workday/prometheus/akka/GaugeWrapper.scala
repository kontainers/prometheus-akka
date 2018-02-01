package com.workday.prometheus.akka

import java.util.concurrent.atomic.DoubleAdder
import java.util.function.ToDoubleFunction

import scala.collection.JavaConverters._

import io.micrometer.core.instrument.{ MeterRegistry, Tag}

case class GaugeWrapper(registry: MeterRegistry, name: String, tags: Iterable[Tag]) {
  private val adder = new DoubleAdder
  private val fn = new ToDoubleFunction[DoubleAdder] {
    override def applyAsDouble(value: DoubleAdder): Double = value.doubleValue
  }
  registry.gauge(name, tags.asJava, adder, fn)
  def decrement(): Unit = increment(-1.0)
  def increment(): Unit = increment(1.0)
  def increment(d: Double): Unit = adder.add(d)
}
