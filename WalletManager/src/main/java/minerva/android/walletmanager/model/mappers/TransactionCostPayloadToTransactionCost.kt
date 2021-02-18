package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.GasPrices
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.defs.getTransactionType
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxSpeed
import java.math.BigDecimal

class TransactionCostPayloadToTransactionCost(
    private val gasPrices: GasPrices?,
    private val chainId: Int,
    val convert: (value: BigDecimal) -> BigDecimal
) : Mapper<TransactionCostPayload, TransactionCost> {

    override fun map(input: TransactionCostPayload): TransactionCost {
        return gasPrices?.let {
            input.run {
                TransactionCost(
                    gasPrice = gasPrice,
                    gasLimit = gasLimit,
                    cost = cost,
                    txSpeeds = listOf(
                        TxSpeed(TxType.RAPID, convert(gasPrices.speed.rapid)),
                        TxSpeed(TxType.FAST, convert(gasPrices.speed.fast)),
                        TxSpeed(TxType.STANDARD, convert(gasPrices.speed.standard)),
                        TxSpeed(TxType.SLOW, convert(gasPrices.speed.slow))
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
}