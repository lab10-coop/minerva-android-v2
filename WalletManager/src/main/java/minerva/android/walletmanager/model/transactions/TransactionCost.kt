package minerva.android.walletmanager.model.transactions

import minerva.android.blockchainprovider.defs.Operation
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.BigInteger

data class TransactionCost(
    val gasPrice: BigDecimal = BigDecimal.ZERO,
    val gasLimit: BigInteger = BigInteger.ONE,
    val cost: BigDecimal = Double.InvalidValue.toBigDecimal(),
    val fiatCost: String = String.Empty,
    val txSpeeds: List<TxSpeed> = listOf()
) {
    val isGasLimitDefaultValue
        get() = gasLimit == Operation.TRANSFER_NATIVE.gasLimit
    val formattedCryptoCost = BalanceUtils.getCryptoBalance(cost)
}