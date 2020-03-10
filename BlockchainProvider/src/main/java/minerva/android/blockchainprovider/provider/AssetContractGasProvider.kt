package minerva.android.blockchainprovider.provider

import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class AssetContractGasProvider (private val price: BigInteger, private val limit: BigInteger) : ContractGasProvider {
    override fun getGasLimit(contractFunc: String?): BigInteger =limit

    override fun getGasLimit(): BigInteger = limit

    override fun getGasPrice(contractFunc: String?): BigInteger = price

    override fun getGasPrice(): BigInteger = price
}