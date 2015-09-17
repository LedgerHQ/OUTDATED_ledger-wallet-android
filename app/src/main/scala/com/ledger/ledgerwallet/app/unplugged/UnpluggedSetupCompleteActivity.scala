/**
 *
 * HomeActivity
 * Ledger wallet
 *
 * Created by Nicolas Bigot on 10/02/15.
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
package com.ledger.ledgerwallet.app.unplugged

import android.os.Bundle
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view._
import android.widget.ImageView
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.app.Config
import com.ledger.ledgerwallet.base.BaseFragment
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.utils.{AndroidUtils, TR}
import com.ledger.ledgerwallet.widget.{SpacerItemDecoration, TextView}

class UnpluggedSetupCompleteActivity extends UnpluggedSetupActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    setContentFragment(new ContentFragment())
  }

  private class ContentFragment extends BaseFragment {

    val MyceliumActionId = 0x01
    val GreenBitsActionId = 0x02

    val Actions = Array(
      new Action(
        id = MyceliumActionId,
        title = R.string.unplugged_existing_install_mycelium,
        subtitle = R.string.unplugged_existing_view_on_play_store,
        icon = R.drawable.ic_mycelium
      ),
      new Action(
        id = GreenBitsActionId,
        title = R.string.unplugged_existing_install_greenbits,
        subtitle = R.string.unplugged_existing_view_on_play_store,
        icon = R.drawable.ic_greenbits
      )
    )

    lazy val actionsView = findView[RecyclerView](R.id.actions)

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_setup_complete_activity, container, false)
    }

    def onActionClick(actionId: Int): Unit = {
      actionId match {
        case MyceliumActionId => AndroidUtils.startMarketApplicationPage(Config.MyceliumPackageName)
        case GreenBitsActionId => AndroidUtils.startMarketApplicationPage(Config.GreenBitsPackageName)
      }
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      actionsView.setAdapter(new ActionsAdapter)
      actionsView.setLayoutManager(new LinearLayoutManager(this))
      actionsView.setItemAnimator(new DefaultItemAnimator)
      actionsView.addItemDecoration(new SpacerItemDecoration(TR(R.dimen.large_margin).as[Float].toInt))
    }

    case class Action(id: Int, title: Int, subtitle: Int, icon: Int)

    private[this] class ActionsAdapter extends RecyclerView.Adapter[ViewHolder] {

      lazy val inflater = LayoutInflater.from(getActivity)

      override def getItemCount: Int = Actions.length

      override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = {
        val action = Actions(position)
        holder.title.setText(action.title)
        holder.subtitle.setText(action.subtitle)
        holder.icon.setImageResource(action.icon)
        holder.view.onClick {
          onActionClick(action.id)
        }
      }

      override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = {
        new ViewHolder(inflater.inflate(R.layout.action_list_item, parent, false))
      }
    }

    private[this] class ViewHolder(val view: View) extends RecyclerView.ViewHolder(view) {
      lazy val title = view.findViewById(R.id.title).asInstanceOf[TextView]
      lazy val subtitle = view.findViewById(R.id.subtitle).asInstanceOf[TextView]
      lazy val icon = view.findViewById(R.id.icon).asInstanceOf[ImageView]

    }

  }

}

