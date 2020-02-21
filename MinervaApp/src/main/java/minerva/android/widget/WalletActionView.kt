package minerva.android.widget

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.wallet_action_item_list_row.view.*
import minerva.android.R
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.CHANGED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.LOG_IN
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.RECEIVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

class WalletActionView(context: Context) : ConstraintLayout(context) {

    init {
        inflate(context, R.layout.wallet_action_item_list_row, this)
    }

    fun setActionType(walletAction: WalletAction) {
        when (walletAction.type) {
            WalletActionType.IDENTITY -> showAction(
                walletAction.fields[WalletActionFields.IDENTITY_NAME],
                R.string.identity_name,
                R.drawable.ic_identities
            )
            WalletActionType.VALUE -> showValues(walletAction)
            WalletActionType.SERVICE -> showServices(walletAction)
        }
    }

    private fun showAction(name: String?, message: Int, actionIcon: Int) {
        type.text = context.getString(message, "<$name>")
        showIcon(actionIcon)
    }

    private fun showValues(walletAction: WalletAction) {
        when (walletAction.status) {
            FAILED -> showSentAction(walletAction, R.string.failed_sent_values_label)
            SENT -> showSentAction(walletAction, R.string.sent_values_label)
            ADDED, REMOVED -> showAction(
                walletAction.fields[WalletActionFields.VALUE_NAME],
                R.string.value_added,
                R.drawable.ic_values
            )
        }
    }

    private fun showServices(walletAction: WalletAction) {
        type.text = context.getString(
            R.string.services_login_label,
            "<${walletAction.fields[WalletActionFields.IDENTITY_NAME]}>",
            "<${walletAction.fields[WalletActionFields.SERVICE]}>"
        )
        showIcon(R.drawable.ic_services)
    }

    private fun showSentAction(walletAction: WalletAction, text: Int) {
        type.text = context.getString(
            text,
            "<${walletAction.fields[WalletActionFields.AMOUNT]}",
            "${walletAction.fields[WalletActionFields.NETWORK]}>",
            "<${walletAction.fields[WalletActionFields.RECEIVER]}>"
        )
        showIcon(R.drawable.ic_values)
    }

    private fun showIcon(actionIcon: Int) {
        Glide.with(context).load(actionIcon).into(icon)
    }

    fun setActionStatus(walletAction: WalletAction) {
        val lastUsed = DateUtils.getTimeFromTimeStamp(walletAction.lastUsed)
        context.run {
            when (walletAction.status) {
                REMOVED -> header.text = getString(R.string.wallet_action_header, getString(R.string.removed), lastUsed)
                CHANGED -> header.text = getString(R.string.wallet_action_header, getString(R.string.changed), lastUsed)
                ADDED -> header.text = getString(R.string.wallet_action_header, getString(R.string.added), lastUsed)
                RECEIVED -> header.text = getString(R.string.wallet_action_header, getString(R.string.received), lastUsed)
                SENT -> header.text = getString(R.string.wallet_action_header, getString(R.string.sent), lastUsed)
                FAILED -> header.text = getString(R.string.wallet_action_header, getString(R.string.failed), lastUsed)
                LOG_IN -> header.text = getString(R.string.wallet_action_header, getString(R.string.logIn), lastUsed)
            }
        }
    }
}