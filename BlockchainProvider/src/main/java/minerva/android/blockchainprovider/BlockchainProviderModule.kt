package minerva.android.blockchainprovider

import org.koin.dsl.module

fun createBlockchainProviderModule(blockchainUrl: String) = module {
    factory { Web3jProvider.provideWeb3j(blockchainUrl) }
    factory { BlockchainRepository(get()) }
}