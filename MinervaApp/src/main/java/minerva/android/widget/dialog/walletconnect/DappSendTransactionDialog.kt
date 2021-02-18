package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.transition.TransitionManager
import androidx.core.view.isVisible
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding
import minerva.android.extension.visibleOrInvisible
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.TxSpeed
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import java.math.BigDecimal

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
        }
    }

    fun setContent(
        transaction: WalletConnectTransaction,
        session: DappSession,
        account: Account?,
        showGasPriceDialog: () -> Unit,
        recalculateTxCost: (BigDecimal) -> WalletConnectTransaction
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
        }
        editTxTime.setOnClickListener { showGasPriceDialog() }

        gasPriceSelector.setAdapter(transaction.txCost.txSpeeds) {
            setTxCost(recalculateTxCost(it.value), account)
        }

        setTxCost(transaction, account)
        value.text = transaction.fiatWithUnit

        closeCustomTime.setOnClickListener {
            TransitionManager.beginDelayedTransition(sendTransactionDialog)
            closeCustomTime.visibleOrInvisible(false)
            editTxTime.visibleOrInvisible(true)
            gasPriceSelector.visibleOrInvisible(true)
            speed.visibleOrInvisible(false)
            transactionTime.visibleOrInvisible(false)
            setTxCost(transaction, account)
        }
    }

    fun setCustomGasPrice(transaction: WalletConnectTransaction, account: Account?) = with(binding) {
        TransitionManager.beginDelayedTransition(sendTransactionDialog)
        closeCustomTime.visibleOrInvisible(true)
        editTxTime.visibleOrInvisible(false)
        speed.visibleOrInvisible(true)
        transactionTime.visibleOrInvisible(true)
        gasPriceSelector.visibleOrInvisible(false)
        setTxCost(transaction, account)
    }

    private fun setTxCost(transaction: WalletConnectTransaction, account: Account?) = with(binding) {
        "${transaction.txCost.formattedCryptoCost} ${account?.network?.token} (${transaction.txCost.fiatCost})".also {
            transactionCost.text = it
        }
    }
}