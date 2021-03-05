package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount

object PendingTransactionToPendingAccountMapper : Mapper<PendingTransaction, PendingAccount> {
    override fun map(input: PendingTransaction): PendingAccount =
        PendingAccount(
            input.index,
            input.txHash,
            input.chainId,
            input.senderAddress,
            input.blockHash,
            input.amount,
            input.blockNumber
        )
}