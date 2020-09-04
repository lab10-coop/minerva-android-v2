package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Transaction

object TransactionToTransactionPayloadMapper : Mapper<Transaction, TransactionPayload> {
    override fun map(input: Transaction): TransactionPayload =
        input.run {
            TransactionPayload(
                address,
                privateKey,
                receiverKey,
                amount,
                gasPrice,
                gasLimit,
                contractAddress
            )
        }
}