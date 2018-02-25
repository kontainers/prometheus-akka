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
package akka.monitor.instrumentation

import java.util.concurrent.TimeUnit

import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory

import com.workday.prometheus.akka._

import akka.actor.{ActorRef, ActorSystem, Cell}
import akka.monitor.instrumentation.ActorMonitors.{TrackedActor, TrackedRoutee}

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
    val actorMetrics = if (cellInfo.isTracked) ActorMetrics.metricsFor(cellInfo.entity) else None
    new TrackedActor(cellInfo.entity, cellInfo.actorSystemName, actorMetrics, cellInfo.trackingGroups, cellInfo.actorCellCreation)
  }

  def createRouteeMonitor(cellInfo: CellInfo): ActorMonitor = {
    RouterMetrics.metricsFor(cellInfo.entity) match {
      case Some(rm) =>
        new TrackedRoutee(cellInfo.entity, cellInfo.actorSystemName, rm, cellInfo.trackingGroups, cellInfo.actorCellCreation)
      case _ =>
        new TrackedActor(cellInfo.entity, cellInfo.actorSystemName, None, cellInfo.trackingGroups, cellInfo.actorCellCreation)
    }
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
        am.mailboxSize.increment()
        am.messages.increment()
      }
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timeInMailbox: Long = System.nanoTime() - envelopeContext.nanoTime

      val actorProcessingTimers = actorMetrics.map { am =>
        am.processingTime.startTimer()
      }
      val actorGroupProcessingTimers = trackingGroups.map { group =>
        ActorGroupMetrics.processingTime(group).startTimer()
      }

      try {
        pjp.proceed()
      } finally {
        actorProcessingTimers.foreach { _.close() }
        actorGroupProcessingTimers.foreach { _.close() }

        actorMetrics.foreach { am =>
          am.timeInMailbox.timer.record(timeInMailbox, TimeUnit.NANOSECONDS)
          am.mailboxSize.decrement()
        }
        recordGroupMetrics(timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      actorMetrics.foreach { am =>
        am.errors.increment()
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
      routerMetrics.messages.increment()
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timeInMailbox: Long = System.nanoTime() - envelopeContext.nanoTime

      val processingTimer = routerMetrics.processingTime.startTimer()
      val actorGroupProcessingTimers = trackingGroups.map { group =>
        ActorGroupMetrics.processingTime(group).startTimer()
      }

      try {
        pjp.proceed()
      } finally {
        processingTimer.close()
        actorGroupProcessingTimers.foreach { _.close() }
        routerMetrics.timeInMailbox.timer.record(timeInMailbox, TimeUnit.NANOSECONDS)
        recordGroupMetrics(timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      routerMetrics.errors.increment()
      super.processFailure(failure)
    }
  }

  abstract class GroupMetricsTrackingActor(entity: Entity, actorSystemName: String,
      trackingGroups: List[String], actorCellCreation: Boolean) extends ActorMonitor {
    if (actorCellCreation) {
      ActorSystemMetrics.actorCount(actorSystemName).increment()
      trackingGroups.foreach { group =>
        ActorGroupMetrics.actorCount(group).increment()
      }
    }

    def captureEnvelopeContext(): EnvelopeContext = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.mailboxSize(group).increment()
        ActorGroupMetrics.messages(group).increment()
      }
      EnvelopeContext()
    }

    protected def recordGroupMetrics(timeInMailbox: Long): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.timeInMailbox(group).timer.record(timeInMailbox, TimeUnit.NANOSECONDS)
        ActorGroupMetrics.mailboxSize(group).decrement()
      }
    }

    def processFailure(failure: Throwable): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.errors(group).increment()
      }
    }

    def cleanup(): Unit = {
      if (actorCellCreation) {
        ActorSystemMetrics.actorCount(actorSystemName).decrement()
        trackingGroups.foreach { group =>
          ActorGroupMetrics.actorCount(group).decrement()
        }
      }
    }

  }
}
