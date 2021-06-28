package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TransactionSpeed
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.defs.getTransactionType
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxSpeed
import java.math.BigDecimal

object TransactionCostPayloadToTransactionCost {

    fun map(
        input: TransactionCostPayload,
        transactionSpeed: TransactionSpeed?,
        chainId: Int,
        convert: (value: BigDecimal) -> BigDecimal
    ): TransactionCost =
        transactionSpeed?.let {
            input.run {
                TransactionCost(
                    gasPrice = gasPrice,
                    gasLimit = gasLimit,
                    cost = cost,
                    txSpeeds = listOf(
                        TxSpeed(TxType.RAPID, convert(transactionSpeed.rapid)),
                        TxSpeed(TxType.FAST, convert(transactionSpeed.fast)),
                        TxSpeed(TxType.STANDARD, convert(transactionSpeed.standard)),
                        TxSpeed(TxType.SLOW, convert(transactionSpeed.slow))
                    )
                )
            }
        }.orElse {
            input.run {
                TransactionCost(
                    gasPrice,
                    gasLimit,
                    cost,
                    txSpeeds = listOf(TxSpeed(getTransactionType(chainId), input.gasPrice))
                )
            }
        }
}