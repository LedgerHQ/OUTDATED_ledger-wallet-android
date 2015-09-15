/**
 *
 * SeedTextView
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 15/09/15.
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
package com.ledger.ledgerwallet.widget

import android.content.Context
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView
import android.text._
import android.text.style.{CharacterStyle, ForegroundColorSpan, SuggestionSpan}
import android.util.AttributeSet
import android.widget.{ArrayAdapter, MultiAutoCompleteTextView}
import android.widget.MultiAutoCompleteTextView.Tokenizer
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.bitcoin.Bip39Helper
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.utils.logs.Logger
import com.rockymadden.stringmetric.similarity.{WeightedLevenshteinMetric, LevenshteinMetric}

class Bip39MnemonicPhraseEditText(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
  extends AppCompatMultiAutoCompleteTextView(context, attrs) {

  def this(context: Context, attrs: AttributeSet, defStyleAttr: Int) = this(context, attrs, 0, 0)
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

  setTokenizer(new Tokenizer {
    override def findTokenStart(s: CharSequence, cursor: Int): Int = {
      var i = cursor
      while (i > 0 && s.charAt(i - 1) != ' ') {
        i -= 1
      }
      i
    }

    override def findTokenEnd(s: CharSequence, cursor: Int): Int = {
      var i = 0
      val length = s.length()
      while (i < length) {
        if (s.charAt(i) == ' ')
          return i
        i += 1
      }
      length
    }

    override def terminateToken(s: CharSequence): CharSequence = s

  })

  setAdapter(new ArrayAdapter[String](getContext, android.R.layout.simple_dropdown_item_1line, Bip39Helper.EnglishWords))

  addTextChangedListener(new TextWatcher {

    override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}

    override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}

    override def afterTextChanged(s: Editable): Unit = {
      val words = s.toString.split(' ')
      var reminder = 0
      val text = s.toString
      val selection = getSelectionStart
      for ((word, index) <- words.zipWithIndex) {
        Logger.d(s"Got word  $word")
        // Check if we need to clean up the string
        if (word.length == 0) {
          cleanString(s, words)
          return
        }
        val start = text.indexOf(word, reminder)
        val end = start + word.length
        reminder = end

        val spans = s.getSpans(start, end, classOf[CharacterStyle])

        if ((selection < start || selection > end) &&
            spans.indexWhere({_.isInstanceOf[Bip39WordErrorSpan]}) == -1 &&
            !Bip39Helper.EnglishWords.contains(word)) {
          s.setSpan(new Bip39WordErrorSpan, start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        } else if (spans.indexWhere({_.isInstanceOf[Bip39WordErrorSpan]}) != -1) {
          s.removeSpan(spans(spans.indexWhere({_.isInstanceOf[Bip39WordErrorSpan]})))
        }

        // If last word followed by a space, try autocomplete
        if (index == words.length - 1 && s.charAt(s.length() - 1) == ' ') {
          val bestMatch = Bip39Helper.EnglishWords
            .map({(s: String) =>
              val lev: Option[Double] = WeightedLevenshteinMetric(10, 0.1, 1).compare(s, word.toString)
              (s, lev)
          }).filter({ (tuple: (String, Option[Double])) =>
              tuple._2.isDefined
          }).sortWith(_._2.get < _._2.get).map(_._1).headOption
          if (bestMatch.isDefined && !bestMatch.get.equals(word)) {
            s.replace(start, end, bestMatch.get)
          }
        }

      }

    }

    private def cleanString(s: Editable, words: Array[String]): Unit = {
      val (cleanedString, _) = words.zipWithIndex.fold((new SpannableStringBuilder(), 0)) {(r, item) =>
        val (text: CharSequence, _) = r
        val (word: CharSequence, index: Int) = item
        if (word.length() > 0 && index < words.length - 1) {
          val builder = text.asInstanceOf[SpannableStringBuilder]
          val start = builder.length()
          val end = start + word.length()
          builder.append(word)
          s.getSpans(start, end, classOf[CharacterStyle]).foreach { (span: CharacterStyle) =>
            builder.setSpan(span, start, end, builder.getSpanFlags(span))
          }
          builder.append(' ')
        }
        r
      }
      if (s.length() > 0 && s.charAt(s.length() - 1) != ' ') {
        s.delete(s.length() - 1, s.length())
      }
      Logger.d(s"Cleaned is $cleanedString")
      s.replace(0, s.length(), cleanedString)
    }

  })

  private class Bip39WordSuggestionSpan(words: Array[String], flags: Int) extends SuggestionSpan(getContext, words, flags)
  private class Bip39WordErrorSpan extends ForegroundColorSpan(TR(getContext, R.color.invalid_red).asColor)
}