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
            from,
            to,
            amount,
            allowance,
            chainId,
            tokenDecimals,
            contractAddress,
            contractData
        )
    }

    private fun mapTransactionType(transferType: TransferType): BlockchainTransactionType =
        when (transferType) {
            TransferType.COIN_TRANSFER -> BlockchainTransactionType.COIN_TRANSFER
            TransferType.TOKEN_TRANSFER -> BlockchainTransactionType.TOKEN_TRANSFER
            TransferType.TOKEN_SWAP -> BlockchainTransactionType.TOKEN_SWAP
            TransferType.TOKEN_SWAP_APPROVAL -> BlockchainTransactionType.TOKEN_SWAP_APPROVAL
            TransferType.COIN_SWAP -> BlockchainTransactionType.COIN_SWAP
            TransferType.SAFE_ACCOUNT_COIN_TRANSFER -> BlockchainTransactionType.SAFE_ACCOUNT_COIN_TRANSFER
            TransferType.SAFE_ACCOUNT_TOKEN_TRANSFER -> BlockchainTransactionType.SAFE_ACCOUNT_TOKEN_TRANSFER
            TransferType.DEFAULT_TOKEN_TX -> BlockchainTransactionType.DEFAULT_TOKEN_TX
            TransferType.DEFAULT_COIN_TX -> BlockchainTransactionType.DEFAULT_COIN_TX
            TransferType.UNKNOWN -> BlockchainTransactionType.UNKNOWN
            TransferType.ERC721_TRANSFER -> BlockchainTransactionType.ERC721_TRANSFER
            TransferType.ERC1155_TRANSFER -> BlockchainTransactionType.ERC1155_TRANSFER
        }
}