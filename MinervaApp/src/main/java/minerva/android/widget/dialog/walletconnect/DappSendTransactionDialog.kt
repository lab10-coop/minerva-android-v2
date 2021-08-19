package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.transition.TransitionManager
import androidx.core.view.isVisible
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import minerva.android.walletmanager.utils.BalanceUtils
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
        recalculateTxCost: (BigDecimal) -> WalletConnectTransaction,
        isBalanceTooLow: (balance: BigDecimal, cost: BigDecimal) -> Boolean
    ) = with(binding) {
        val networkName = account?.network?.name ?: session.networkName
        setupHeader(session.name, networkName, session.iconUrl)
        prepareTransactions(transaction, account)
        senderAddress.text = transaction.from
        receiverAddress.text = transaction.to
        receiverAddressFull.text = transaction.to
        account?.let {
            accountName.text = it.name
            "${BalanceUtils.getCryptoBalance(it.cryptoBalance)} ${it.network.token}".also { text -> balance.text = text }

            if (isBalanceTooLow(it.cryptoBalance, transaction.txCost.cost)) {
                errorView.visible()
                balance.setTextColor(context.getColor(R.color.errorRed))
            }
        }
        editTxTime.setOnClickListener { showGasPriceDialog() }
        gasPriceSelector.setAdapter(transaction.txCost.txSpeeds) { setTxCost(recalculateTxCost(it.value), account) }
        gasPriceSelector.setDefaultPosition(getDefaultTxType(account?.chainId))
        setTxCost(transaction, account)
        transaction.fiatValue?.let { value.text = it }
        closeCustomTime.setOnClickListener {
            TransitionManager.beginDelayedTransition(sendTransactionDialog)
            closeCustomTime.visibleOrInvisible(false)
            editTxTime.visibleOrInvisible(true)
            gasPriceSelector.visibleOrInvisible(true)
            speed.visibleOrInvisible(false)
            transactionTime.visibleOrInvisible(false)
            setTxCost(transaction, account)
            handleBalanceTooLow(account, isBalanceTooLow, transaction)
        }
    }


    private fun getDefaultTxType(chainId: Int?) = if (isMaticNetwork(chainId)) TxType.STANDARD else TxType.FAST

    private fun isMaticNetwork(chainId: Int?) = chainId == ChainId.MATIC

    private fun prepareTransactions(transaction: WalletConnectTransaction, account: Account?) = with(binding) {
        when (transaction.transactionType) {
            TransferType.TOKEN_SWAP_APPROVAL -> {
                value.invisible()
                unit.text = transaction.tokenTransaction.tokenSymbol
                requestLabel.text = context.getString(R.string.pre_authorize)
                receiver.text = context.getText(R.string.allowance_receiver)
                transactionType.text = context.getText(R.string.allowance)
                amount.text =
                    if (transaction.tokenTransaction.allowance == Double.InvalidValue.toBigDecimal()) context.getString(R.string.unlimited)
                    else transaction.tokenTransaction.allowance?.toPlainString()

            }
            TransferType.TOKEN_SWAP -> {
                value.invisible()
                amount.text = transaction.tokenTransaction.tokenValue
                unit.text = transaction.tokenTransaction.tokenSymbol
            }
            TransferType.DEFAULT_COIN_TX -> receiverGroup.gone()
            TransferType.DEFAULT_TOKEN_TX -> {
                receiverGroup.gone()
                transferGroup.gone()
            }
            else -> {
                amount.text = transaction.value
                account?.let { unit.text = it.network.token }
            }
        }
    }


    fun setCustomGasPrice(
        transaction: WalletConnectTransaction,
        account: Account?,
        isBalanceToLow: (balance: BigDecimal, cost: BigDecimal) -> Boolean
    ) =
        with(binding) {
            TransitionManager.beginDelayedTransition(sendTransactionDialog)
            closeCustomTime.visibleOrInvisible(true)
            editTxTime.visibleOrInvisible(false)
            speed.visibleOrInvisible(true)
            transactionTime.visibleOrInvisible(true)
            gasPriceSelector.visibleOrInvisible(false)
            setTxCost(transaction, account)
            handleBalanceTooLow(account, isBalanceToLow, transaction)
        }

    private fun handleBalanceTooLow(
        account: Account?,
        isBalanceTooLow: (balance: BigDecimal, cost: BigDecimal) -> Boolean,
        transaction: WalletConnectTransaction
    ) = with(binding) {
        account?.let {
            if (isBalanceTooLow(it.cryptoBalance, transaction.txCost.cost)) {
                errorView.visible()
                balance.setTextColor(context.getColor(R.color.errorRed))
            } else {
                errorView.invisible()
                balance.setTextColor(context.getColor(R.color.dappLabelColorGray))
            }
        }
    }

    private fun setTxCost(transaction: WalletConnectTransaction, account: Account?) = with(binding) {
        "${transaction.txCost.formattedCryptoCost} ${account?.network?.token} (${transaction.txCost.fiatCost})".also {
            transactionCost.text = it
        }
    }
}