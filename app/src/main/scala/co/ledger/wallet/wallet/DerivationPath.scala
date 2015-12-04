/**
 *
 * DerivationPath
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 04/12/15.
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
package co.ledger.wallet.wallet

import scala.annotation.tailrec

class DerivationPath(p: DerivationPath, val childNum: Long) {

  lazy val depth: Int = parent.depth + 1
  lazy val parent = p

  def /(child: DerivationPath): DerivationPath = {
    new DerivationPath(this, child.childNum)
  }

  def apply(depth: Int): Option[DerivationPath] = {
    var root = this
    while (root.parent != DerivationPath.Root && root.depth > depth) {
      root = root.parent
    }
    if (root == DerivationPath.Root)
      None
    else
      Some(root)
  }

}

object DerivationPath {

  object Root extends DerivationPath(null, -1) {

    override lazy val parent = this
    override lazy val depth = -1

  }

  def apply(path: String): DerivationPath = {
    @tailrec
    def parse(path: String, node: DerivationPath): DerivationPath = {
      val part = path.takeWhile(_ != '/')
      if (part.contains("m"))
        Root
      else {
        val num = part.takeWhile(_.isDigit)
        if (num.length == 0)
          node
        else {
          val hardened = part.endsWith("'") || part.endsWith("h")
          val childNum = num.toInt.toLong + (if (hardened) 0x80000000L else 0)
          parse(path.substring(part.length), new DerivationPath(node, childNum))
        }
      }
    }
    parse(path, Root)
  }

  object dsl {

    implicit class DPInt(val num: Int) {
      def h: DerivationPath = new DerivationPath(Root, num + 0x80000000)
    }

    implicit def Int2DerivationPath(num: Int): DerivationPath = new DerivationPath(Root, num)

  }

}