package minerva.android.blockchainprovider

import org.koin.dsl.module

fun createBlockchainProviderModule(blockchainUrl: Map<String, String>) = module {
    factory { Web3jProvider.provideWeb3j(blockchainUrl) }
    factory { BlockchainRepositoryImpl(get()) as BlockchainRepository}
}