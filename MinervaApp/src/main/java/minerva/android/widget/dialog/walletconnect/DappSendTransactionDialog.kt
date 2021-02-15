package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.transition.TransitionManager
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding
import minerva.android.extension.toggleVisibleOrGone
import minerva.android.extension.visibleOrInvisible
import minerva.android.utils.BalanceUtils
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

class DappSendTransactionDialog(context: Context, approve: () -> Unit, deny: () -> Unit, showDialog: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappSendTransactionDialogBinding = DappSendTransactionDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        with(binding) {
            setContentView(root)
            initButtons(confirmationButtons)
            editTxTime.setOnClickListener {
                showDialog()
            }

            expandAddressIcon.setOnClickListener {
                TransitionManager.beginDelayedTransition(sendTransactionDialog)
                receiverAddressFull.visibleOrInvisible(!receiverAddressFull.isVisible)
                receiverAddress.visibleOrInvisible(!receiverAddress.isVisible)
                hideAddressIcon.visibleOrInvisible(!hideAddressIcon.isVisible)
            }
        }
    }

    fun setContent(transaction: WalletConnectTransaction, session: DappSession, account: Account?) = with(binding) {
        setupHeader(session.name, session.networkName, session.iconUrl)
        amount.text = transaction.value
        senderAddress.text = transaction.from
        receiverAddress.text = transaction.to
        receiverAddressFull.text = transaction.to
        account?.let {
            unit.text = it.network.token
            accountName.text = it.name
            "${BalanceUtils.getCryptoBalance(it.cryptoBalance)} ${it.network.token}".also { text -> balance.text = text }
        }
    }


}