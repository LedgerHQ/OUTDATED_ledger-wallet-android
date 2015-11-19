/**
 *
 * BaseModel
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 23/01/15.
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
package co.ledger.wallet.app.base.model

import java.util.Date

import org.json.{JSONException, JSONObject}

import scala.collection.mutable

class BaseModel {

  private[this] val _structure = new mutable.HashMap[String, Property[_]]()
  def structure = _structure

  protected def string(name: String): StringProperty = new StringProperty(name)
  protected def int(name: String): IntProperty = new IntProperty(name)
  protected def long(name: String): LongProperty = new LongProperty(name)
  protected def boolean(name: String): BooleanProperty = new BooleanProperty(name)
  protected def double(name: String): DoubleProperty = new DoubleProperty(name)
  protected def date(name: String): DateProperty = new DateProperty(name)

  def toJson: JSONObject = {
    val structure = this.structure
    val json = new JSONObject()
    try {
      structure.foreach { case (key, value) =>
        if (value.isDefined) {
          value match {
            case string: StringProperty => json.put(key, string.get)
            case int: IntProperty => json.put(key, int.get)
            case boolean: BooleanProperty => json.put(key, boolean.get)
            case double: DoubleProperty => json.put(key, double.get)
            case long: LongProperty => json.put(key, long.get)
            case date: DateProperty => json.put(key, date.get.asInstanceOf[Date].getTime)
          }
        }
      }
    } catch {
      case json: JSONException => null
    }
    json
  }

  def apply(propertyName: String): Property[_] = structure(propertyName)

  class Property[T](val name: String) {
    structure(name) = this

    private var _value: T = _

    def get: T = _value
    def set(value: T): this.type = {
      _value = value
      this
    }
    def isEmpty = _value == null
    def isDefined = !isEmpty
  }

  class StringProperty(name: String) extends Property[String](name)
  class IntProperty(name: String) extends  Property[Int](name)
  class DoubleProperty(name: String) extends Property[Double](name)
  class BooleanProperty(name: String) extends Property[Boolean](name)
  class LongProperty(name: String) extends Property[Long](name)
  class DateProperty(name: String) extends Property[Date](name)

}