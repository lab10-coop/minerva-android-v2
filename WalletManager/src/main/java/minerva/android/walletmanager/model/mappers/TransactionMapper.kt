package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Transaction

object TransactionMapper : Mapper<Transaction, TransactionPayload> {
    override fun map(input: Transaction): TransactionPayload =
        TransactionPayload(
            contractAddress = input.contractAddress,
            privateKey = input.privateKey,
            receiverKey = input.receiverKey,
            amount = input.amount,
            gasLimit = input.gasLimit,
            gasPrice = input.gasPrice
        )
}