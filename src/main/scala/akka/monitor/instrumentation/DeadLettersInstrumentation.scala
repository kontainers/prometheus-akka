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

import akka.actor.{ActorSystem, DeadLetter, UnhandledMessage}
import org.aspectj.lang.annotation.{After, Aspect, DeclareMixin, Pointcut}

import com.workday.prometheus.akka.{ActorSystemMetrics, MetricsConfig}

trait HasSystem {
  def system: ActorSystem
  def setSystem(system: ActorSystem): Unit
}

object HasSystem {
  def apply(): HasSystem = new HasSystem {
    private var _system: ActorSystem = _

    override def system: ActorSystem = _system

    override def setSystem(system: ActorSystem): Unit = _system = system
  }
}

@Aspect
class DeadLettersInstrumentation {

  @DeclareMixin("akka.event.EventStream+")
  def mixinHasSystem: HasSystem = HasSystem()

  @Pointcut("execution(akka.event.EventStream.new(..)) && this(eventStream) && args(system, debug)")
  def eventStreamCreation(eventStream: HasSystem, system: ActorSystem, debug: Boolean): Unit = {}

  @After("eventStreamCreation(eventStream, system, debug)")
  def aroundEventStreamCreation(eventStream: HasSystem, system: ActorSystem, debug: Boolean): Unit = {
    eventStream.setSystem(system)
  }

  @Pointcut("execution(* akka.event.EventStream.publish(..)) && this(stream) && args(event)")
  def streamPublish(stream: HasSystem, event: AnyRef): Unit = {}

  @After("streamPublish(stream, event)")
  def afterStreamSubchannel(stream: HasSystem, event: AnyRef): Unit = trackEvent(stream, event)

  private def trackEvent(stream: HasSystem, event: AnyRef): Unit = {
    if (MetricsConfig.matchEvents) {
      event match {
        case _: DeadLetter => ActorSystemMetrics.deadLetterCount(stream.system.name).increment()
        case _: UnhandledMessage => ActorSystemMetrics.unhandledMessageCount(stream.system.name).increment()
        case _ =>
      }
    }
  }

}