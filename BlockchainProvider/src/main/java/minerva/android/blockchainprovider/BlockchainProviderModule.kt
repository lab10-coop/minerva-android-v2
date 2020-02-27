package minerva.android.blockchainprovider

import org.koin.dsl.module

fun createBlockchainProviderModule(blockchainUrl: Map<String, String>, ensUrl: String) = module {
    factory { Web3jProvider.provideWeb3j(blockchainUrl.toMutableMap(), ensUrl) }
    factory { BlockchainRepositoryImpl(get()) as BlockchainRepository}
}