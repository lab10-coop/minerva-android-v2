package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletConnect.model.ethereum.WCEthereumTransaction
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

object WCEthTransactionToWalletConnectTransactionMapper : Mapper<WCEthereumTransaction, WalletConnectTransaction> {
    override fun map(input: WCEthereumTransaction): WalletConnectTransaction = with(input) {
        WalletConnectTransaction(
            from = from,
            to = to,
            nonce = nonce,
            gasPrice = gasPrice,
            gasLimit = gasLimit,
            value = value,
            fiatValue = null,
            data = data
        )
    }
}