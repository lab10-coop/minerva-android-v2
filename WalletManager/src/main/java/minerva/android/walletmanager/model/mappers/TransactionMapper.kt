package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.transactions.Transaction

object TransactionMapper : Mapper<Transaction, TransactionPayload> {
    override fun map(input: Transaction): TransactionPayload =
        TransactionPayload(
            contractAddress = input.contractAddress,
            privateKey = input.privateKey,
            receiverAddress = input.receiverKey,
            amount = input.amount,
            gasLimit = input.gasLimit,
            gasPrice = input.gasPrice,
            tokenDecimals = input.tokenDecimals,
            tokenId = input.tokenId
        )
}