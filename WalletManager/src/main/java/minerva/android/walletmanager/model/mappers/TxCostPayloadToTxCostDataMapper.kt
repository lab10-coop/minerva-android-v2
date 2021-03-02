package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.blockchainprovider.model.TxCostData
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.transactions.TxCostPayload

object TxCostPayloadToTxCostDataMapper : Mapper<TxCostPayload, TxCostData> {
    override fun map(input: TxCostPayload): TxCostData = with(input) {
        TxCostData(
            mapTransactionType(transferType),
            networkShort,
            from,
            to,
            amount,
            chainId,
            contractAddress,
            contractData
        )
    }

    private fun mapTransactionType(transferType: TransferType): BlockchainTransactionType =
        when (transferType) {
            TransferType.COIN_TRANSFER -> BlockchainTransactionType.COIN_TRANSFER
            TransferType.TOKEN_TRANSFER -> BlockchainTransactionType.TOKEN_TRANSFER
            TransferType.TOKEN_SWAP -> BlockchainTransactionType.TOKEN_SWAP
            TransferType.COIN_SWAP -> BlockchainTransactionType.COIN_SWAP
            TransferType.SAFE_ACCOUNT_COIN_TRANSFER -> BlockchainTransactionType.SAFE_ACCOUNT_COIN_TRANSFER
            TransferType.SAFE_ACCOUNT_TOKEN_TRANSFER -> BlockchainTransactionType.SAFE_ACCOUNT_TOKEN_TRANSFER
            TransferType.UNDEFINED -> BlockchainTransactionType.UNDEFINED
        }
}