package minerva.android.walletmanager.model.transactions

import minerva.android.blockchainprovider.defs.Operation
import java.math.BigDecimal
import java.math.BigInteger

data class TransactionCost(
    val gasPrice: BigDecimal = BigDecimal.ZERO,
    val gasLimit: BigInteger = BigInteger.ONE,
    val cost: BigDecimal = BigDecimal.ZERO,
    val txSpeeds: List<TxSpeed> = listOf()
) {
    val isGasLimitDefaultValue
        get() = gasLimit == Operation.TRANSFER_NATIVE.gasLimit
}