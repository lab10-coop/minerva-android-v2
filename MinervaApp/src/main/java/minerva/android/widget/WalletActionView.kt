package minerva.android.widget

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.WalletActionItemListRowBinding
import minerva.android.kotlinUtils.DateUtils
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.UNDEFINED
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ACCEPTED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.AUTHORISED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.BACKGROUND_ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.CHANGED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.LOG_IN
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.RECEIVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REJECTED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SA_ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SIGNED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.UPDATED
import minerva.android.walletmanager.model.defs.WalletActionType

class WalletActionView(context: Context) : ConstraintLayout(context) {

    private val binding = WalletActionItemListRowBinding.bind(inflate(context, R.layout.wallet_action_item_list_row, this))

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
        binding.type.text = context.getString(message, "$name")
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
            SA_ADDED, SAFE_ACCOUNT_REMOVED -> showAction(
                walletAction.fields[WalletActionFields.ACCOUNT_NAME],
                R.string.safe_account_action_label,
                R.drawable.ic_values
            )
        }
    }

    private fun showServices(walletAction: WalletAction) {
        binding.type.text = when (walletAction.status) {
            ADDED -> context.getString(R.string.service_added, walletAction.fields[WalletActionFields.SERVICE_NAME])
            BACKGROUND_ADDED -> context.getString(
                R.string.background_service_added,
                walletAction.fields[WalletActionFields.SERVICE_NAME]
            )
            SENT -> context.getString(
                R.string.service_sent,
                walletAction.fields[WalletActionFields.CREDENTIAL_NAME],
                walletAction.fields[WalletActionFields.SERVICE_NAME]
            )
            AUTHORISED, SIGNED ->
                context.getString(R.string.service_action_label, "${walletAction.fields[WalletActionFields.SERVICE_NAME]}")
            LOG_IN ->
                context.getString(
                    R.string.services_login_label,
                    "${walletAction.fields[WalletActionFields.IDENTITY_NAME]}",
                    "${walletAction.fields[WalletActionFields.SERVICE_NAME]}"
                )
            REMOVED -> walletAction.fields[WalletActionFields.SERVICE_NAME]
            REJECTED, ACCEPTED -> context.getString(
                R.string.connection_request,
                walletAction.fields[WalletActionFields.SERVICE_NAME]
            )
            else -> UNDEFINED
        }
        showIcon(R.drawable.ic_services)
    }

    private fun showSentAction(walletAction: WalletAction, text: Int) {
        binding.type.text = context.getString(
            text,
            "${walletAction.fields[WalletActionFields.AMOUNT]}",
            "${walletAction.fields[WalletActionFields.TOKEN]}",
            "${walletAction.fields[WalletActionFields.RECEIVER]}"
        )
        showIcon(R.drawable.ic_values)
    }

    private fun showIcon(actionIcon: Int) {
        Glide.with(context).load(actionIcon).into(binding.mainIcon)
    }

    fun setActionStatus(walletAction: WalletAction) {
        val lastUsed = DateUtils.getTimeFromTimeStamp(walletAction.lastUsed)
        context.run {
            binding.title.text = when (walletAction.status) {
                REMOVED, SAFE_ACCOUNT_REMOVED -> getString(R.string.wallet_action_header, getString(R.string.removed), lastUsed)
                CHANGED -> getString(R.string.wallet_action_header, getString(R.string.changed), lastUsed)
                ADDED, SA_ADDED, BACKGROUND_ADDED -> getString(R.string.wallet_action_header, getString(R.string.added), lastUsed)
                RECEIVED -> getString(R.string.wallet_action_header, getString(R.string.received), lastUsed)
                SENT -> getString(R.string.wallet_action_header, getString(R.string.sent), lastUsed)
                FAILED -> getString(R.string.wallet_action_header, getString(R.string.failed), lastUsed)
                LOG_IN -> getString(R.string.wallet_action_header, getString(R.string.logIn), lastUsed)
                AUTHORISED -> getString(R.string.wallet_action_header, getString(R.string.authorised), lastUsed)
                SIGNED -> getString(R.string.wallet_action_header, getString(R.string.signed), lastUsed)
                UPDATED -> getString(R.string.wallet_action_header, getString(R.string.updated), lastUsed)
                REJECTED -> getString(R.string.wallet_action_header, getString(R.string.rejected), lastUsed)
                ACCEPTED -> getString(R.string.wallet_action_header, getString(R.string.accepted), lastUsed)
                else -> UNDEFINED
            }
        }
    }
}