package minerva.android.blockchainprovider.repository.contract

import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import minerva.android.blockchainprovider.contract.GnosisSafe
import minerva.android.blockchainprovider.contract.ProxyFactory
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.GNOSIS_SETUP_DATA
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.MASTER_COPY_KEY
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.PROXY_ADDRESS
import minerva.android.blockchainprovider.provider.SmartContractGasProvider
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

class SmartContractRepositoryImpl(private val web3j: Map<String, Web3j>) : SmartContractRepository {

    override fun deployGnosisSafeContract(privateKey: String, address: String, network: String): Single<String> =
        getProxyFactory(network, privateKey)
            .createProxy(MASTER_COPY_KEY, getGnosisSetupData(address))
            .flowable()
            .singleOrError()
            .flatMap { getProxyFactory(network, privateKey).getProxyCreationEvents(it).toFlowable().singleOrError() }
            .map { it.proxy }

    private fun getGnosisSetupData(address: String) = String.format(GNOSIS_SETUP_DATA, address.removePrefix(HEX_PREFIX)).toByteArray()

    private fun getProxyFactory(network: String, privateKey: String): ProxyFactory =
        ProxyFactory.load(PROXY_ADDRESS, web3j[network], Credentials.create(privateKey), SmartContractGasProvider())

//    TODO check if invocation of gnosis contract is correct
    private fun getGnosisSmartContract(gnosisAddress: String, network: String, privateKey: String): GnosisSafe =
        GnosisSafe.load(gnosisAddress, web3j[network], Credentials.create(privateKey), SmartContractGasProvider())

    companion object {
        private const val HEX_PREFIX = "0x"
    }
}