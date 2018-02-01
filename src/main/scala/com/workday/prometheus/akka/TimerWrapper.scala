package com.workday.prometheus.akka

import java.io.Closeable
import java.util.concurrent.TimeUnit

import io.micrometer.core.instrument.Timer

case class TimerWrapper(timer: Timer) {

  class TimeObservation(timer: Timer, startTime: Long) extends Closeable {
    def close(): Unit = timer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
  }

  def startTimer(): TimeObservation = new TimeObservation(timer, System.nanoTime())
}
