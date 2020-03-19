package minerva.android.blockchainprovider.repository.contract

import java.math.BigInteger

object GnosisSafeHelper {
    //TODO data to make GnosisSafe contract work, should be determined i different way
    val data = ByteArray(0)
    val operation: BigInteger = BigInteger.valueOf(0)
    val safeTxGas: BigInteger = BigInteger.valueOf(50_000)
    val baseGas: BigInteger = BigInteger.valueOf(300_000)
    val gasPrice: BigInteger = BigInteger.valueOf(0)
    val noFunds: BigInteger = BigInteger.valueOf(0)
    const val gasToken = "0x0000000000000000000000000000000000000000"
    const val refund = "0x0000000000000000000000000000000000000000"
    const val safeSentinelAddress = "0x0000000000000000000000000000000000000001"
}