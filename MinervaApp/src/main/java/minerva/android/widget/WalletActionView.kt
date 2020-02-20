package minerva.android.widget

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.wallet_action_item_list_row.view.*
import minerva.android.R
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

class WalletActionView(context: Context) : ConstraintLayout(context) {

    init {
        inflate(context, R.layout.wallet_action_item_list_row, this)
    }

    fun setTime(lastUsed: Long) {
        time.text = DateUtils.getTimeFromTimeStamp(lastUsed)
    }

    fun setActionType(walletAction: WalletAction) {
        when (walletAction.type) {
            WalletActionType.IDENTITY -> shoIdentities(walletAction)
            WalletActionType.VALUE -> type.text = context.getString(R.string.value)
            WalletActionType.SERVICE -> type.text = context.getString(R.string.service)
        }
    }

    fun setActionStatus(walletAction: WalletAction) {
        when (walletAction.status) {
            WalletActionStatus.REMOVED -> status.text = context.getString(R.string.removed)
            WalletActionStatus.CHANGED -> status.text = context.getString(R.string.changed)
            WalletActionStatus.ADDED -> status.text = context.getString(R.string.added)
            WalletActionStatus.RECEIVED -> status.text = context.getString(R.string.received)
            WalletActionStatus.SENT -> status.text = context.getString(R.string.sent)
            WalletActionStatus.FAILED -> status.text = context.getString(R.string.failed)
            WalletActionStatus.LOG_IN -> status.text = context.getString(R.string.logIn)
        }
    }

    private fun shoIdentities(walletAction: WalletAction) {
        type.text = context.getString(R.string.identity_name, "<${walletAction.fields[WalletActionFields.INDENTITY_NAME]}>")
        Glide.with(context).load(R.drawable.ic_identities).into(icon)
    }
}