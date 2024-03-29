package minerva.android.blockchainprovider.defs

import java.math.BigInteger

enum class Operation(val gasLimit: BigInteger) {
    TRANSFER_NATIVE(TRANSFER_NATIVE_LIMIT),
    TRANSFER_ERC20(TRANSFER_ERC20_LIMIT),
    SAFE_ACCOUNT_TXS(SAFE_ACCOUNT_TXS_LIMIT),
    DEFAULT_LIMIT(DEFAULT_GAS_LIMIT),
    TRANSFER_ERC721(TRANSFER_ERC721_LIMIT),
    TRANSFER_ERC1155(TRANSFER_ERC1155_LIMIT),
}

private val TRANSFER_NATIVE_LIMIT = BigInteger.valueOf(50000)
private val TRANSFER_ERC20_LIMIT = BigInteger.valueOf(200000)
private val SAFE_ACCOUNT_TXS_LIMIT = BigInteger.valueOf(350000)
private val DEFAULT_GAS_LIMIT = BigInteger.valueOf(21000)
private val TRANSFER_ERC721_LIMIT = BigInteger.valueOf(300000)
private val TRANSFER_ERC1155_LIMIT = BigInteger.valueOf(300000)