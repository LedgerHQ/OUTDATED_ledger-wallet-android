/**
 *
 * HomeActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 10/02/15.
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
package co.ledger.wallet.app.unplugged

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view._
import android.widget.ImageView
import co.ledger.wallet.R
import co.ledger.wallet.base.BaseFragment
import co.ledger.wallet.common._
import co.ledger.wallet.utils.TR
import co.ledger.wallet.widget.{SpacerItemDecoration, TextView}

class UnpluggedWelcomeActivity extends UnpluggedSetupActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    setContentFragment(new ContentFragment())
  }

  private class ContentFragment extends BaseFragment {

    val CreateActionId = 0x01
    val RestoreActionId = 0x02

    val Actions = Array(
      new Action(
        id = CreateActionId,
        title = R.string.unplugged_welcome_use_as_new,
        subtitle = R.string.unplugged_welcome_if_its_your_first_wallet,
        icon = R.drawable.ic_wallet
      ),
      new Action(
        id = RestoreActionId,
        title = R.string.unplugged_welcome_recover_wallet,
        subtitle = R.string.unplugged_welcome_if_you_lost_your_wallet,
        icon = R.drawable.ic_restore
      )
    )

    lazy val actionsView = findView[RecyclerView](R.id.actions)

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_welcome_activity, container, false)
    }

    def onActionClick(actionId: Int): Unit = {
      setupMode = actionId match {
        case CreateActionId => UnpluggedSetupActivity.CreateWalletSetupMode
        case RestoreActionId => UnpluggedSetupActivity.RestoreWalletSetupMode
      }
      startNextActivity(classOf[UnpluggedSecurityActivity])
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

