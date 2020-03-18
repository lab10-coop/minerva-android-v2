package minerva.android.blockchainprovider.provider

import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class SmartContractGasProvider : ContractGasProvider {
    override fun getGasLimit(contractFunc: String?): BigInteger = GAS_LIMIT

    override fun getGasLimit(): BigInteger = GAS_LIMIT

    override fun getGasPrice(contractFunc: String?): BigInteger =
        DEFAULT_GAS_PRICE

    override fun getGasPrice(): BigInteger =
        DEFAULT_GAS_PRICE

    companion object {
        private val DEFAULT_GAS_PRICE = BigInteger.valueOf(1000_000_000L)
        private val GAS_LIMIT = BigInteger.valueOf(300_000L)
    }
}