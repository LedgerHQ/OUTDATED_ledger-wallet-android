/**
 *
 * JsonUtils
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 29/01/15.
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
package com.ledger.ledgerwallet.utils

import org.json.{JSONArray, JSONObject}
import scala.collection.Map

trait JsonUtils {

  implicit def Map2JsonObject[T](map: Map[String, T]): JSONObject = {
    val json = new JSONObject()
    map foreach {case (key, value) =>
      value match {
        case string: String => json.put(key, string)
        case double: Double => json.put(key, double)
        case float: Float => json.put(key, float)
        case boolean: Boolean => json.put(key, boolean)
        case jsonObject: JSONObject => json.put(key, jsonObject)
        case jsonArray: JSONArray => json.put(key, jsonArray)
        case map: Map[_, _] => json.put(key, Map2JsonObject(map.asInstanceOf[Map[String, _]]))
        case array: Array[_] => json.put(key, Array2JsonArray(array))
        case _ => json.put(key, value.toString)
      }
    }
    json
  }

  implicit def Array2JsonArray[T](array: Array[T]): JSONArray = {
    val json = new JSONArray()
    array foreach {
      case string: String => json.put(string)
      case double: Double => json.put(double)
      case float: Float => json.put(float)
      case boolean: Boolean => json.put(boolean)
      case jsonObject: JSONObject => json.put(jsonObject)
      case jsonArray: JSONArray => json.put(jsonArray)
      case map: Map[_, _] => json.put(Map2JsonObject(map.asInstanceOf[Map[String, _]]))
      case array: Array[AnyRef] => json.put(Array2JsonArray(array))
      case value => json.put(value.toString)
    }
    json
  }

  implicit class JsonStringContext(val c: StringContext) {
    def json(args: Any*): JSONObject = {
      val strings = c.parts.iterator
      val arguments = args.iterator
      val string = new StringBuffer(strings.next())
      while (strings.hasNext) {
        arguments.next() match {
          case charSequence: CharSequence => string.append("\"" + charSequence.toString + "\"")
          case arg => string.append(arg.toString)
        }
        string.append(strings.next())
      }
      new JSONObject(string.toString)
    }
  }

}

object JsonUtils extends JsonUtils
