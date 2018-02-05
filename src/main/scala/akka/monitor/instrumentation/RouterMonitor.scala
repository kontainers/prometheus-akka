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

import org.aspectj.lang.ProceedingJoinPoint

import com.workday.prometheus.akka.{Entity, RouterMetrics}

import akka.actor.Cell

trait RouterMonitor {
  def processMessage(pjp: ProceedingJoinPoint): AnyRef
  def processFailure(failure: Throwable): Unit
  def cleanup(): Unit

  def routeeAdded(): Unit
  def routeeRemoved(): Unit
}

object RouterMonitor {

  def createRouterInstrumentation(cell: Cell): RouterMonitor = {
    val cellInfo = CellInfo.cellInfoFor(cell, cell.system, cell.self, cell.parent, false)
    if (cellInfo.isTracked) {
      RouterMetrics.metricsFor(cellInfo.entity) match {
        case Some(rm) => new MetricsOnlyRouterMonitor(cellInfo.entity, rm)
        case _ => NoOpRouterMonitor
      }
    }
    else {
      NoOpRouterMonitor
    }
  }
}

object NoOpRouterMonitor extends RouterMonitor {
  def processMessage(pjp: ProceedingJoinPoint): AnyRef = pjp.proceed()
  def processFailure(failure: Throwable): Unit = {}
  def routeeAdded(): Unit = {}
  def routeeRemoved(): Unit = {}
  def cleanup(): Unit = {}
}

class MetricsOnlyRouterMonitor(entity: Entity, routerMetrics: RouterMetrics) extends RouterMonitor {

  def processMessage(pjp: ProceedingJoinPoint): AnyRef = {
    val timer = routerMetrics.routingTime.startTimer()
    try {
      pjp.proceed()
    } finally {
      timer.close()
    }
  }

  def processFailure(failure: Throwable): Unit = {}
  def routeeAdded(): Unit = {}
  def routeeRemoved(): Unit = {}
  def cleanup(): Unit = {}
}
