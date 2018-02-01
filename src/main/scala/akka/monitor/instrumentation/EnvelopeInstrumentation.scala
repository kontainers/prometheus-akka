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

import org.aspectj.lang.annotation.{ DeclareMixin, Aspect }

case class EnvelopeContext(nanoTime: Long)

object EnvelopeContext {
  val Empty = EnvelopeContext(0L)
  def apply(): EnvelopeContext = EnvelopeContext(System.nanoTime())
}

trait InstrumentedEnvelope extends Serializable {
  def envelopeContext(): EnvelopeContext
  def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit
}

object InstrumentedEnvelope {
  def apply(): InstrumentedEnvelope = new InstrumentedEnvelope {
    var envelopeContext: EnvelopeContext = EnvelopeContext.Empty

    override def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit =
      this.envelopeContext = envelopeContext
  }
}

@Aspect
class EnvelopeContextIntoEnvelopeMixin {

  @DeclareMixin("akka.dispatch.Envelope")
  def mixinInstrumentationToEnvelope: InstrumentedEnvelope = InstrumentedEnvelope()
}
