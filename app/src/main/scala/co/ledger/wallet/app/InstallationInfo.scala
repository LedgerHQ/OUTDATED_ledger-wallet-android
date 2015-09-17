/**
 *
 * InstallationInfo
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/03/15.
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
package co.ledger.wallet.app

import java.util.UUID

import android.content.Context
import co.ledger.wallet.utils.Preferenceable

/**
 * Gathers all properties for the current application installation
 */
object InstallationInfo extends Preferenceable {

  private var _uuid: Option[UUID] = None

  def uuid(implicit context: Context): UUID = _uuid.getOrElse {
      val uuidString = Option(preferences.getString("uuid", null))
      _uuid = Some(uuidString.map(UUID.fromString).getOrElse(UUID.randomUUID()))
      preferences.edit().putString("uuid", _uuid.get.toString).commit()
      _uuid.get
  }

  override def PreferencesName: String = "InstallationInfo"
}
