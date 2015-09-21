/**
 *
 * Collection
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 26/01/15.
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
package co.ledger.wallet.base.model

import java.util.Date

import org.json.{JSONArray, JSONObject}

import scala.reflect.ClassTag

class Collection[T <: BaseModel](implicit T: ClassTag[T]) {

  def inflate(jsonObject: JSONObject): Option[T] = {
    val obj = T.runtimeClass.newInstance().asInstanceOf[T]
    obj.structure.foreach {case (key, property) =>
       if (jsonObject.has(key)) {
         property match {
           case string: obj.StringProperty => string.set(jsonObject.getString(key))
           case int: obj.IntProperty => int.set(jsonObject.getInt(key))
           case double: obj.DoubleProperty => double.set(jsonObject.getDouble(key))
           case float: obj.BooleanProperty => float.set(jsonObject.getBoolean(key))
           case date: obj.DateProperty => date.set(new Date(jsonObject.getLong(key)))
           case long: obj.LongProperty => long.set(jsonObject.getLong(key))
         }
       }
    }
    Option(obj)
  }

  def inflate(jsonArray: JSONArray): Array[Option[T]] = {
    val array = new Array[Option[T]](jsonArray.length())
    for (i <- 0 until jsonArray.length()) {
      array(i) = inflate(jsonArray.getJSONObject(i))
    }
    array
  }

  def toJson(obj: BaseModel) = obj.toJson
  def toJson(objs: Array[BaseModel]): JSONArray = {
    val array = new JSONArray()
    for (obj <- objs) {
      array.put(obj.toJson)
    }
    array
  }

}

object Collection extends Collection[BaseModel] {

}