package minerva.android.widget

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.wallet_action_item_list_row.view.*
import minerva.android.R
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.UNDEFINED
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.AUTHORISED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.CHANGED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.LOG_IN
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.RECEIVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SIGNED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.UPDATED
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
            WalletActionType.ACCOUNT -> showAccounts(walletAction)
            WalletActionType.SERVICE -> showServices(walletAction)
            WalletActionType.CREDENTIAL -> showCredentials(walletAction)
        }
    }

    private fun showCredentials(walletAction: WalletAction) {
        when (walletAction.status) {
            ADDED, REMOVED, UPDATED -> showAction(
                walletAction.fields[WalletActionFields.CREDENTIAL_NAME],
                R.string.credential_action_label,
                R.drawable.ic_identities //TODO change to credential icon
            )
            FAILED -> showAction(
                walletAction.fields[WalletActionFields.CREDENTIAL_NAME],
                R.string.credential_import_label,
                R.drawable.ic_identities //TODO change to credential icon
            )
        }
    }

    private fun showAction(name: String?, message: Int, actionIcon: Int) {
        type.text = context.getString(message, "$name")
        showIcon(actionIcon)
    }

    private fun showAccounts(walletAction: WalletAction) {
        when (walletAction.status) {
            FAILED -> showSentAction(walletAction, R.string.failed_sent_accounts_label)
            SENT -> showSentAction(walletAction, R.string.sent_accounts_label)
            ADDED, REMOVED -> showAction(
                walletAction.fields[WalletActionFields.ACCOUNT_NAME],
                R.string.account_action_label,
                R.drawable.ic_values
            )
            SAFE_ACCOUNT_ADDED, SAFE_ACCOUNT_REMOVED -> showAction(
                walletAction.fields[WalletActionFields.ACCOUNT_NAME],
                R.string.safe_account_action_label,
                R.drawable.ic_values
            )
        }
    }

    private fun showServices(walletAction: WalletAction) {
        type.text = when (walletAction.status) {
            AUTHORISED, SIGNED ->
                context.getString(R.string.service_action_label, "${walletAction.fields[WalletActionFields.SERVICE_NAME]}")
            LOG_IN ->
                context.getString(
                    R.string.services_login_label,
                    "${walletAction.fields[WalletActionFields.IDENTITY_NAME]}",
                    "${walletAction.fields[WalletActionFields.SERVICE_NAME]}"
                )
            REMOVED -> walletAction.fields[WalletActionFields.SERVICE_NAME]
            else -> UNDEFINED
        }
        showIcon(R.drawable.ic_services)
    }

    private fun showSentAction(walletAction: WalletAction, text: Int) {
        type.text = context.getString(
            text,
            "${walletAction.fields[WalletActionFields.AMOUNT]}",
            "${walletAction.fields[WalletActionFields.NETWORK]}",
            "${walletAction.fields[WalletActionFields.RECEIVER]}"
        )
        showIcon(R.drawable.ic_values)
    }

    private fun showIcon(actionIcon: Int) {
        Glide.with(context).load(actionIcon).into(icon)
    }

    fun setActionStatus(walletAction: WalletAction) {
        val lastUsed = DateUtils.getTimeFromTimeStamp(walletAction.lastUsed)
        context.run {
            header.text = when (walletAction.status) {
                REMOVED, SAFE_ACCOUNT_REMOVED -> getString(R.string.wallet_action_header, getString(R.string.removed), lastUsed)
                CHANGED -> getString(R.string.wallet_action_header, getString(R.string.changed), lastUsed)
                ADDED, SAFE_ACCOUNT_ADDED -> getString(R.string.wallet_action_header, getString(R.string.added), lastUsed)
                RECEIVED -> getString(R.string.wallet_action_header, getString(R.string.received), lastUsed)
                SENT -> getString(R.string.wallet_action_header, getString(R.string.sent), lastUsed)
                FAILED -> getString(R.string.wallet_action_header, getString(R.string.failed), lastUsed)
                LOG_IN -> getString(R.string.wallet_action_header, getString(R.string.logIn), lastUsed)
                AUTHORISED -> getString(R.string.wallet_action_header, getString(R.string.authorised), lastUsed)
                SIGNED -> getString(R.string.wallet_action_header, getString(R.string.signed), lastUsed)
                UPDATED -> getString(R.string.wallet_action_header, getString(R.string.updated), lastUsed)
                else -> UNDEFINED
            }
        }
    }
}