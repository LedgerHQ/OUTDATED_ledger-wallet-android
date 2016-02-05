/**
 *
 * DemoWalletHomeFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 01/02/16.
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
package co.ledger.wallet.app.demo

import android.os.Bundle
import android.support.v7.widget.RecyclerView.{AdapterDataObserver, ViewHolder}
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import co.ledger.wallet.R
import co.ledger.wallet.core.adapter.OperationRecyclerViewAdapter
import co.ledger.wallet.core.adapter.OperationRecyclerViewAdapter.OperationViewHolder
import co.ledger.wallet.core.base.{BaseFragment, WalletActivity}
import co.ledger.wallet.core.concurrent.AsyncCursor
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.wallet.events.WalletEvents
import co.ledger.wallet.wallet.{Account, Operation}
import org.bitcoinj.core.Coin
import WalletEvents._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DemoWalletHomeFragment extends BaseFragment
  with ViewFinder
  with MainThreadEventReceiver {

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_wallet_home_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    if (_adapter.isDefined)
      recyclerView.setAdapter(_adapter.get)
    else
      setupAdapter()
    recyclerView.setLayoutManager(new LinearLayoutManager(this))
  }

  def setupAdapter(): Unit = {
    wallet.operations() flatMap {(cursor) =>
      wallet.accounts().map((cursor, _))
    } onComplete {
      case Success((cursor, accounts)) =>
        _adapter = Some(new HomeRecyclerViewAdapter(cursor, accounts))
        recyclerView.setAdapter(_adapter.get)
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    register(wallet.eventBus)
  }


  override def onPause(): Unit = {
    super.onPause()
    unregisterAll()
  }

  override implicit def viewId2View[V <: View](id: Int): V = getView.findViewById(id)
    .asInstanceOf[V]

  def wallet = getActivity.asInstanceOf[WalletActivity].wallet
  def recyclerView = getView.asInstanceOf[RecyclerView]

  override def receive: Receive = {
    case CoinSent(_, coin) => setupAdapter()
    case CoinReceived(_, coin) => setupAdapter()
    case NewOperation(_, _) => setupAdapter()
    case OperationChanged(_, _) => setupAdapter()
    case ignore =>
  }

  private var _adapter: Option[HomeRecyclerViewAdapter] = None

  class HomeRecyclerViewAdapter(operationCursor: AsyncCursor[Operation],
                                accounts: Array[Account])
    extends RecyclerView.Adapter[ViewHolder] {

    val operationsAdapter = new OperationRecyclerViewAdapter(operationCursor)
    val accountsAdapter = new AccountRecyclerViewAdapter(accounts)

    override def getItemCount: Int = operationsAdapter.getItemCount + accountsAdapter
      .getItemCount + 2


    override def getItemViewType(position: Int): Int = {
      super.getItemViewType(position)
      if (position == 0 || position == accountsAdapter.getItemCount + 1)
        0
      else if (position <= accountsAdapter.getItemCount)
        1
      else
        2
    }

    override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = {
      getItemViewType(position) match {
        case 0 =>
          if (position == 0)
            holder.asInstanceOf[TitleViewHolder].update("Accounts")
          else
            holder.asInstanceOf[TitleViewHolder].update("Last operations")
        case 1 => accountsAdapter.onBindViewHolder(holder, position - 1)
        case 2 => operationsAdapter.onBindViewHolder(holder.asInstanceOf[OperationViewHolder],
          position - 2 - accountsAdapter.getItemCount)
      }
    }

    override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = {
      viewType match {
        case 0 =>
          val inflater = LayoutInflater.from(parent.getContext)
          val v = inflater.inflate(R.layout.demo_title_list_item, parent, false)
          new TitleViewHolder(v)
        case 1 => accountsAdapter.onCreateViewHolder(parent, viewType)
        case 2 => operationsAdapter.onCreateViewHolder(parent, viewType)
      }
    }

    operationsAdapter.registerAdapterDataObserver(new AdapterDataObserver {
      override def onChanged(): Unit = notifyDataSetChanged()
    })
    accountsAdapter.registerAdapterDataObserver(new AdapterDataObserver {
      override def onChanged(): Unit = notifyDataSetChanged()
    })

  }

  class AccountRecyclerViewAdapter(accounts: Array[Account])
    extends RecyclerView.Adapter[ViewHolder] {

    var balanceCache = Map[String, Future[Coin]]()

    override def getItemCount: Int = accounts.length

    override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = {
      val account = accounts(position)
      val name = s"Account #${account.index}"
      val futureCoin = balanceCache.lift(name)
      val coin = futureCoin.flatMap(_.value.flatMap(_.toOption))
      holder.asInstanceOf[AccountViewHolder].update(name, coin)
      if (futureCoin.isEmpty || futureCoin.get.value.get.isFailure) {
        val future = account.balance()
        balanceCache += name -> future
        future onComplete {
          case all => notifyDataSetChanged()
        }
      }
    }

    override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = {
      val inflater = LayoutInflater.from(parent.getContext)
      val v = inflater.inflate(R.layout.demo_account_list_item, parent, false)
      new AccountViewHolder(v)
    }
  }

  class TitleViewHolder(v: View) extends ViewHolder(v) {

    val text = v.findViewById(R.id.title).asInstanceOf[TextView]

    def update(title: String): Unit = {
      text.setText(title.toUpperCase)
    }

  }

  class AccountViewHolder(v: View) extends ViewHolder(v) {

    val name = v.findViewById(R.id.name).asInstanceOf[TextView]
    val balance = v.findViewById(R.id.balance).asInstanceOf[TextView]

    def update(name: String, balance: Option[Coin]): Unit = {
      this.name.setText(name)
      balance match {
        case Some(coin) => this.balance.setText(coin.toFriendlyString)
        case None => this.balance.setText("")
      }
    }

  }

}
