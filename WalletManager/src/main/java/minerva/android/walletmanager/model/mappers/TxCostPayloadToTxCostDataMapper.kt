package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TxCostData
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.transactions.TxCostPayload

object TxCostPayloadToTxCostDataMapper : Mapper<TxCostPayload, TxCostData> {
    override fun map(input: TxCostPayload): TxCostData = with(input) {
        TxCostData(
            networkShort,
            tokenIndex,
            from,
            to,
            amount,
            chainId,
            contractAddress,
            contractData
        )
    }
}