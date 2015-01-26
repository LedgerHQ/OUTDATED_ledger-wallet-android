/**
 *
 * BaseModelSerializationTests
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
package com.ledger.ledgerwallet.base.model

import android.test.InstrumentationTestCase
import junit.framework.Assert
import org.json.{JSONArray, JSONObject}
import scala.collection.JavaConversions._

class BaseModelJsonTest extends InstrumentationTestCase {

  def testModelShouldSerializeSimpleModel(): Unit = {
    val m = new SimpleModel
    m.name.set("Android")
    m.age.set(12)
    val json = new JSONObject()
    json.put("name", "Android")
    json.put("age", 12)

    val mJson = m.toJson
    Assert.assertEquals(json.length(), mJson.length())
    for (key <- json.keys()) {
      Assert.assertEquals(json.get(key), mJson.get(key))
    }
  }

  def testModelShouldBeInflatedFromJson(): Unit = {
    val json = new JSONObject()
    json.put("name", "Android")
    json.put("age", 12)

    val m = SimpleModel.inflate(json)

    Assert.assertTrue(m.isDefined)
    Assert.assertEquals(json.getString("name"), m.get.name.get)
    Assert.assertEquals(json.getInt("age"), m.get.age.get)
  }

  def testModelsShouldInflatedFromJson(): Unit = {
    val jsonArray = new JSONArray()
    for (i <- 0 to 16) {
      val jsonObj = new JSONObject()
      jsonObj.put("name", "Android")
      jsonObj.put("age", 12 + i)
      jsonArray.put(jsonObj)
    }

    val models = SimpleModel.inflate(jsonArray)

    Assert.assertEquals(jsonArray.length(), models.length)
    for (i <- 0 to 16) {
      val m = models(i)
      val json = jsonArray.getJSONObject(i)
      Assert.assertTrue(m.isDefined)
      Assert.assertEquals(json.getString("name"), m.get.name.get)
      Assert.assertEquals(json.getInt("age"), m.get.age.get)
    }

  }

}

class SimpleModel extends BaseModel {
  val name = string("name")
  val age = int("age")
}

object SimpleModel extends Collection[SimpleModel]