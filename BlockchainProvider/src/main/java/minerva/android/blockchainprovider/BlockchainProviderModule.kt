package minerva.android.blockchainprovider

import minerva.android.blockchainprovider.provider.Web3jProvider
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepositoryImpl
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.blockchainprovider.repository.contract.SmartContractRepositoryImpl
import org.koin.dsl.module
import java.math.BigInteger

fun createBlockchainProviderModule(blockchainUrl: Map<String, String>, ensUrl: String, gasPrice: Map<String, BigInteger>) = module {
    factory { Web3jProvider.provideWeb3j(blockchainUrl.toMutableMap(), ensUrl) }
    factory { Web3jProvider.provideEnsResolver(ensUrl) }
    factory<BlockchainRepository> { BlockchainRepositoryImpl(get(), gasPrice, get()) }
    factory<SmartContractRepository> { SmartContractRepositoryImpl(get(), gasPrice) }
}