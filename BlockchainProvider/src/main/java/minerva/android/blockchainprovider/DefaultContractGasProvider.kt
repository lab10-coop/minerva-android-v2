package minerva.android.blockchainprovider

import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

//TODO default hardcoded gas price and limit - should be refactored after MVP
class DefaultContractGasProvider : ContractGasProvider {
    override fun getGasLimit(contractFunc: String?): BigInteger = GAS_LIMIT

    override fun getGasLimit(): BigInteger = GAS_LIMIT

    override fun getGasPrice(contractFunc: String?): BigInteger = DEFAULT_GAS_PRICE

    override fun getGasPrice(): BigInteger = DEFAULT_GAS_PRICE

    companion object {
        private val DEFAULT_GAS_PRICE = BigInteger.valueOf(1000000000)
        private val GAS_LIMIT = BigInteger.valueOf(100000)
    }
}