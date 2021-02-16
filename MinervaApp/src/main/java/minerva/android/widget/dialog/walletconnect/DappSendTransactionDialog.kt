package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.transition.TransitionManager
import androidx.core.view.isVisible
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding
import minerva.android.extension.visibleOrInvisible
import minerva.android.main.walletconnect.transaction.TxSpeed
import minerva.android.utils.BalanceUtils
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

class DappSendTransactionDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappSendTransactionDialogBinding = DappSendTransactionDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        with(binding) {
            setContentView(root)
            initButtons(confirmationButtons)

            expandAddressIcon.setOnClickListener {
                TransitionManager.beginDelayedTransition(sendTransactionDialog)
                receiverAddressFull.visibleOrInvisible(!receiverAddressFull.isVisible)
                receiverAddress.visibleOrInvisible(!receiverAddress.isVisible)
                hideAddressIcon.visibleOrInvisible(!hideAddressIcon.isVisible)
            }

            closeCustomTime.setOnClickListener {
                TransitionManager.beginDelayedTransition(sendTransactionDialog)
                closeCustomTime.visibleOrInvisible(false)
                editTxTime.visibleOrInvisible(true)
                gasPriceSelector.visibleOrInvisible(true)
                speed.visibleOrInvisible(false)
                transactionTime.visibleOrInvisible(false)
            }
        }
    }

    fun setContent(
        transaction: WalletConnectTransaction,
        session: DappSession,
        account: Account?,
        showGasPriceDialog: () -> Unit
    ) = with(binding) {
        setupHeader(session.name, session.networkName, session.iconUrl)
        amount.text = transaction.value
        senderAddress.text = transaction.from
        receiverAddress.text = transaction.to
        receiverAddressFull.text = transaction.to
        account?.let {
            unit.text = it.network.token
            accountName.text = it.name
            "${BalanceUtils.getCryptoBalance(it.cryptoBalance)} ${it.network.token}".also { text -> balance.text = text }
            //todo set tx cost
        }

        editTxTime.setOnClickListener { showGasPriceDialog() }

//todo get list of speeds from api
        gasPriceSelector.setAdapter(
            listOf(
                TxSpeed("Rapid", "~15sec", "23 Gwei"),
                TxSpeed("Fast", "~1min", "24 Gwei"),
                TxSpeed("Standard", "~3min", "25 Gwei"),
                TxSpeed("Slow", ">10min", "26 Gwei")
            )
        )
    }

    fun setCustomGasPrice(gasPrice: String, account: Account?) = with(binding) {
        TransitionManager.beginDelayedTransition(sendTransactionDialog)
        closeCustomTime.visibleOrInvisible(true)
        editTxTime.visibleOrInvisible(false)
        speed.visibleOrInvisible(true)
        transactionTime.visibleOrInvisible(true)
        gasPriceSelector.visibleOrInvisible(false)
        account?.let {
            "$gasPrice ${account.network.token} (8.00 EUR)".also { transactionCost.text = it }
            //todo get eur exchange rate
        }
    }


}