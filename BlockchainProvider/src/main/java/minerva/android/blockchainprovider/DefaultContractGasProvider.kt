package minerva.android.blockchainprovider

import org.web3j.tx.Transfer
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger


class DefaultContractGasProvider : ContractGasProvider {
    override fun getGasLimit(contractFunc: String?): BigInteger = Transfer.GAS_LIMIT

    override fun getGasLimit(): BigInteger = Transfer.GAS_LIMIT

    override fun getGasPrice(contractFunc: String?): BigInteger = DEFAULT_GAS_PRICE

    override fun getGasPrice(): BigInteger = DEFAULT_GAS_PRICE

    companion object {
        private val DEFAULT_GAS_PRICE = BigInteger.valueOf(1000000000)
    }
}