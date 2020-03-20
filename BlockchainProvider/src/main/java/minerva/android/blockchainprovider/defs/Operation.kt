package minerva.android.blockchainprovider.defs

import java.math.BigInteger

enum class Operation(val gasLimit: BigInteger) {
    TRANSFER_NATIVE(TRANSFER_NATIVE_LIMIT),
    TRANSFER_ERC20(TRANSFER_ERC20_LIMIT),
    SAFE_ACCOUNT_TXS(SAFE_ACCOUNT_TXS_LIMIT)
}

private val TRANSFER_NATIVE_LIMIT = BigInteger.valueOf(21000)
private val TRANSFER_ERC20_LIMIT = BigInteger.valueOf(200000)
private val SAFE_ACCOUNT_TXS_LIMIT = BigInteger.valueOf(350000)