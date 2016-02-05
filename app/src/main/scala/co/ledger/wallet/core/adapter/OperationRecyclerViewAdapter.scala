/**
 *
 * OperationRecyclerViewAdapter
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/01/16.
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
package co.ledger.wallet.core.adapter

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.ViewSwitcher
import co.ledger.wallet.R
import co.ledger.wallet.core.adapter.OperationRecyclerViewAdapter.OperationViewHolder
import co.ledger.wallet.core.concurrent.AsyncCursor
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.wallet.Operation
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui

import scala.collection.mutable
import scala.concurrent.Future

class OperationRecyclerViewAdapter(cursor: AsyncCursor[Operation])
extends RecyclerView.Adapter[OperationRecyclerViewAdapter.OperationViewHolder]{

  override def getItemCount: Int = cursor.count

  override def onBindViewHolder(holder: OperationViewHolder, position: Int): Unit = {
    val operation = cursor.item(position)
    holder.update(operation)
    if (operation.isEmpty) {
      val chunkNumber = position / cursor.chunkSize
      if (!_loadedChunks.contains(chunkNumber)) {
        _loadedChunks += chunkNumber
        _lastChunkLoading = _lastChunkLoading.flatMap((_) => cursor.loadChunk(chunkNumber)) map {
          (result) =>
            notifyDataSetChanged()
            result
        } recover {
          case error: Throwable =>
            error.printStackTrace()
            throw error
        }
      }
    }
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder = {
    val v = onCreateView(parent, viewType)
    new OperationViewHolder(v)
  }

  def onCreateView(parent: ViewGroup, viewType: Int): View = {
    LayoutInflater.from(parent.getContext).inflate(R.layout.operation_list_item, parent, false)
  }

  private[this] val _loadedChunks = mutable.HashSet[Int]()
  private[this] var _lastChunkLoading = Future.successful(Array[Operation]())

}

object OperationRecyclerViewAdapter {
  class OperationViewHolder(v: View) extends ViewHolder(v) {

    val switcher = v.asInstanceOf[ViewSwitcher]
    val container = v.findViewById(R.id.operation_container)
    val amount = container.findViewById(R.id.amount).asInstanceOf[TextView]
    val address = container.findViewById(R.id.address).asInstanceOf[TextView]

    def update(operation: Option[Operation]): Unit = {
      operation match {
        case Some(op) =>
          if (op.isReception) {
            address.setText(op.senders.headOption.getOrElse("Unknown"))
            amount.setTextColor(v.getContext.getColor(R.color.valid_green))
            amount.setText("+" + op.value.toFriendlyString)
          } else {
            address.setText(op.recipients.headOption.getOrElse("Unknown"))
            amount.setTextColor(v.getContext.getColor(R.color.invalid_red))
            amount.setText("-" + op.value.toFriendlyString)
          }
          if (op.blockHash == null) {
            v.setBackgroundColor(Color.argb(0xa0, 0xea, 0x2e, 0x49))
          } else {
            v.setBackgroundColor(Color.TRANSPARENT)
          }

          switcher.setDisplayedChild(0)
        case None =>
          switcher.setDisplayedChild(1)
      }
    }

  }
}
