package minerva.android.blockchainprovider

import minerva.android.blockchainprovider.provider.Web3jProvider
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepositoryImpl
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.blockchainprovider.repository.contract.SmartContractRepositoryImpl
import org.koin.dsl.module

fun createBlockchainProviderModule(blockchainUrl: Map<String, String>, ensUrl: String) = module {
    factory { Web3jProvider.provideWeb3j(blockchainUrl.toMutableMap(), ensUrl) }
    factory { BlockchainRepositoryImpl(get()) as BlockchainRepository }
    factory { SmartContractRepositoryImpl(get()) as SmartContractRepository }
}