package minerva.android.blockchainprovider

import org.koin.dsl.module

fun createBlockchainProviderModule(blockchainUrl: String) = module {
    factory { BlockchainProvider(blockchainUrl) }
}