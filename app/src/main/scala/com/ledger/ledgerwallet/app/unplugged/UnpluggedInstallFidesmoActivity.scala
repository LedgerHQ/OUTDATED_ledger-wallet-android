/**
 *
 * UnpluggedInstallFidesmo
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 16/09/15.
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

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ImageView, Toast}
import com.ledger.ledgerwallet.base.BaseFragment
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.nfc.Unplugged
import com.ledger.ledgerwallet.utils.{AndroidUtils, TR}
import com.ledger.ledgerwallet.widget.{SpacerItemDecoration, TextView}
import com.ledger.ledgerwallet.{R, common}

class UnpluggedInstallFidesmoActivity extends UnpluggedSetupActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    setContentFragment(new ContentFragment())
  }

  private class ContentFragment extends BaseFragment {

    val InstallFidesmoActionId = 0x01
    val InstallAppActionId = 0x02

    val Actions = Array(
      new Action(
        id = InstallFidesmoActionId,
        title = R.string.unplugged_fidesmo_install_apk_action_title,
        subtitle = R.string.unplugged_fidesmo_install_apk_action_subtitle,
        icon = R.drawable.ic_fidesmo
      ),
      new Action(
        id = InstallAppActionId,
        title = R.string.unplugged_fidesmo_install_app_action_title,
        subtitle = R.string.unplugged_fidesmo_install_app_action_subtitle,
        icon = R.drawable.ic_ledgerwallet_small
      )
    )

    lazy val actionsView = findView[RecyclerView](R.id.actions)

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_existing_activity, container, false)
    }

    def onActionClick(actionId: Int): Unit = {
      actionId match {
        case InstallFidesmoActionId => AndroidUtils.startMarketApplicationPage(Unplugged.FidesmoAppPackageName)
        case InstallAppActionId =>
          if (AndroidUtils.isPackageInstalled(Unplugged.FidesmoAppPackageName)) {
            val intent = new Intent(Unplugged.FidesmoServiceCardAction, Unplugged.FidesmoServiceUri
              .buildUpon()
              .appendPath(Unplugged.FidesmoAppId)
              .appendPath(Unplugged.FidesmoServiceId)
              .build())
            startActivityForResult(intent, Unplugged.FidesmoServiceRequestCode)
          } else {
            Toast.makeText(this, R.string.unplugged_fidesmo_error_not_installed, Toast.LENGTH_LONG).show()
          }
      }
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      actionsView.setAdapter(new ActionsAdapter)
      actionsView.setLayoutManager(new LinearLayoutManager(this))
      actionsView.setItemAnimator(new DefaultItemAnimator)
      actionsView.addItemDecoration(new SpacerItemDecoration(TR(R.dimen.very_large_margin).as[Float].toInt))
    }


    override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
      super.onActivityResult(requestCode, resultCode, data)
      if (requestCode == Unplugged.FidesmoServiceRequestCode) {
        resultCode match {
          case Activity.RESULT_OK =>
            startActivity(
              new Intent(this, classOf[UnpluggedTapActivity])
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
          case anythingElse =>
            // Do nothing
        }
      }
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
