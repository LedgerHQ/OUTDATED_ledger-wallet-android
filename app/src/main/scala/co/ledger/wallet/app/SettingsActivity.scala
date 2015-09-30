/**
 *
 * SettingsActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 10/09/15.
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

import android.content.Intent
import android.os.Bundle
import android.preference.Preference.{OnPreferenceChangeListener, OnPreferenceClickListener}
import android.preference.{Preference, PreferenceFragment}
import co.ledger.wallet.R
import co.ledger.wallet.base.BaseActivity

class SettingsActivity extends BaseActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    if (getIntent.getBooleanExtra(SettingsActivity.ExtraDisplayLicenses, false)) {
      setContentFragment(new LicensesFragment)
    } else {
      setContentFragment(new SettingsFragment)
    }
  }

  class SettingsFragment extends PreferenceFragment {

    override def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.settings)
      findPreference("licenses").setOnPreferenceClickListener(new OnPreferenceClickListener {
        override def onPreferenceClick(preference: Preference): Boolean = {
          startActivity(
            new Intent(SettingsActivity.this, classOf[SettingsActivity])
            .putExtra(SettingsActivity.ExtraDisplayLicenses, true)
          )
          true
        }
      })
      findPreference("use_internal_keystore").setOnPreferenceChangeListener(new OnPreferenceChangeListener {
        override def onPreferenceChange(preference: Preference, o: scala.Any): Boolean = {
          if (o.equals(true)) {


          }
          true
      }})
    }

  }

  class LicensesFragment extends PreferenceFragment {

    override def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.licenses)
    }

  }

}

object SettingsActivity {

  val ExtraDisplayLicenses = "EXTRA_DISPLAY_LICENCES"


}