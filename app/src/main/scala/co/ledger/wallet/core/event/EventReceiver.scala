/**
 *
 * EventReceiver
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 24/11/15.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package co.ledger.wallet.core.event

import java.lang.ref.WeakReference

import de.greenrobot.event.EventBus

trait EventReceiver {

  type Receive = PartialFunction[AnyRef, Unit]

  def receive: Receive

  def register(eventBus: EventBus): Unit = {
    _eventBuses = _eventBuses :+ new WeakReference(eventBus)
    eventBus.register(this)
  }

  def unregister(eventBus: EventBus): Unit = {
    for (ref <- _eventBuses) {
      if (ref.get() == eventBus) {
        ref.get().unregister(this)
        _eventBuses = _eventBuses.filter(_ != ref)
      }
    }
  }

  def unregisterAll(): Unit = {
    for (eventBus <- _eventBuses) {
      if (eventBus.get() != null) {
        eventBus.get().unregister(this)
      }
    }
    _eventBuses = Array()
  }

  private[this] var _eventBuses = Array[WeakReference[EventBus]]()
}
