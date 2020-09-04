package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.TransactionCost

object TransactionCostPayloadToTransactionCost : Mapper<TransactionCostPayload, TransactionCost> {
    override fun map(input: TransactionCostPayload): TransactionCost =
        input.run { TransactionCost(gasPrice, gasLimit, cost) }
}