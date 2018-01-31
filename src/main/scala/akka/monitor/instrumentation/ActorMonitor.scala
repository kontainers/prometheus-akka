/*
 * =========================================================================================
 * Copyright © 2017 Workday, Inc.
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
package akka.monitor.instrumentation

import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory

import com.workday.prometheus.akka._

import akka.actor.{ActorRef, ActorSystem, Cell}
import akka.monitor.instrumentation.ActorMonitors.{TrackedActor, TrackedRoutee}
import io.prometheus.client.Collector

trait ActorMonitor {
  def captureEnvelopeContext(): EnvelopeContext
  def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef
  def processFailure(failure: Throwable): Unit
  def cleanup(): Unit
}

object ActorMonitor {

  def createActorMonitor(cell: Cell, system: ActorSystem, ref: ActorRef, parent: ActorRef, actorCellCreation: Boolean): ActorMonitor = {
    val cellInfo = CellInfo.cellInfoFor(cell, system, ref, parent, actorCellCreation)

    if (cellInfo.isRouter)
      ActorMonitors.ContextPropagationOnly
    else {
      if (cellInfo.isRoutee && cellInfo.isTracked)
        createRouteeMonitor(cellInfo)
      else
        createRegularActorMonitor(cellInfo)
    }
  }

  def createRegularActorMonitor(cellInfo: CellInfo): ActorMonitor = {
    if (cellInfo.isTracked || cellInfo.trackingGroups.length > 0) {
      val actorMetrics = if (cellInfo.isTracked) Some(ActorMetrics.metricsFor(cellInfo.entity)) else None
      new TrackedActor(cellInfo.entity, cellInfo.actorSystemName, actorMetrics, cellInfo.trackingGroups, cellInfo.actorCellCreation)
    } else {
      ActorMonitors.ContextPropagationOnly
    }
  }

  def createRouteeMonitor(cellInfo: CellInfo): ActorMonitor = {
    def routerMetrics = RouterMetrics.metricsFor(cellInfo.entity)
    new TrackedRoutee(cellInfo.entity, cellInfo.actorSystemName, routerMetrics, cellInfo.trackingGroups, cellInfo.actorCellCreation)
  }
}

object ActorMonitors {

  val logger = LoggerFactory.getLogger(ActorMonitors.getClass)

  val ContextPropagationOnly = new ActorMonitor {
    def captureEnvelopeContext(): EnvelopeContext = EnvelopeContext()

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      pjp.proceed()
    }

    def processFailure(failure: Throwable): Unit = {}
    def cleanup(): Unit = {}
  }

  class TrackedActor(val entity: Entity, actorSystemName: String, actorMetrics: Option[ActorMetrics],
      trackingGroups: List[String], actorCellCreation: Boolean)
      extends GroupMetricsTrackingActor(entity, actorSystemName, trackingGroups, actorCellCreation) {

    if (logger.isDebugEnabled()) {
      logger.debug(s"tracking ${entity.name} actor: ${actorMetrics.isDefined} actor-group: ${trackingGroups}")
    }

    override def captureEnvelopeContext(): EnvelopeContext = {
      actorMetrics.foreach { am =>
        am.mailboxSize.inc()
        am.messages.inc()
      }
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timeInMailbox: Double = (System.nanoTime() - envelopeContext.nanoTime).toDouble / Collector.NANOSECONDS_PER_SECOND

      val actorProcessingTimers = actorMetrics.map { am =>
        am.processingTime.startTimer()
      }
      val actorGroupProcessingTimers = trackingGroups.map { group =>
        ActorGroupMetrics.processingTime.labels(group).startTimer()
      }

      try {
        pjp.proceed()
      } finally {
        actorProcessingTimers.foreach { _.close() }
        actorGroupProcessingTimers.foreach { _.close() }

        actorMetrics.foreach { am =>
          am.timeInMailbox.inc(timeInMailbox)
          am.mailboxSize.dec()
        }
        recordGroupMetrics(timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      actorMetrics.foreach { am =>
        am.errors.inc()
      }
      super.processFailure(failure)
    }
  }

  class TrackedRoutee(val entity: Entity, actorSystemName: String, routerMetrics: RouterMetrics,
      trackingGroups: List[String], actorCellCreation: Boolean)
      extends GroupMetricsTrackingActor(entity, actorSystemName, trackingGroups, actorCellCreation) {

    if (logger.isDebugEnabled()) {
      logger.debug(s"tracking ${entity.name} router: true actor-group: ${trackingGroups}")
    }

    override def captureEnvelopeContext(): EnvelopeContext = {
      routerMetrics.messages.inc()
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timeInMailbox: Double = (System.nanoTime() - envelopeContext.nanoTime).toDouble / Collector.NANOSECONDS_PER_SECOND

      val processingTimer = routerMetrics.processingTime.startTimer()
      val actorGroupProcessingTimers = trackingGroups.map { group =>
        ActorGroupMetrics.processingTime.labels(group).startTimer()
      }

      try {
        pjp.proceed()
      } finally {
        processingTimer.close()
        actorGroupProcessingTimers.foreach { _.close() }
        routerMetrics.timeInMailbox.inc(timeInMailbox)
        recordGroupMetrics(timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      routerMetrics.errors.inc()
      super.processFailure(failure)
    }
  }

  abstract class GroupMetricsTrackingActor(entity: Entity, actorSystemName: String,
      trackingGroups: List[String], actorCellCreation: Boolean) extends ActorMonitor {
    if (actorCellCreation) {
      ActorSystemMetrics.actorCount.labels(actorSystemName).inc()
      trackingGroups.foreach { group =>
        ActorGroupMetrics.actorCount.labels(group).inc()
      }
    }

    def captureEnvelopeContext(): EnvelopeContext = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.mailboxSize.increment()
        ActorGroupMetrics.messages.labels(group).inc()
      }
      EnvelopeContext()
    }

    protected def recordGroupMetrics(timeInMailbox: Double): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.timeInMailbox.labels(group).inc(timeInMailbox)
        ActorGroupMetrics.mailboxSize.labels(group).dec()
      }
    }

    def processFailure(failure: Throwable): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.errors.labels(group).inc()
      }
    }

    def cleanup(): Unit = {
      if (actorCellCreation) {
        ActorSystemMetrics.actorCount.labels(actorSystemName).dec()
        trackingGroups.foreach { group =>
          ActorGroupMetrics.actorCount.labels(group).dec()
        }
      }
    }

  }
}
