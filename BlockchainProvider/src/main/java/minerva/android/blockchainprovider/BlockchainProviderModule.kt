package minerva.android.blockchainprovider

import minerva.android.blockchainprovider.provider.Web3jProvider
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepositoryImpl
import minerva.android.blockchainprovider.repository.smartContract.BlockchainSafeAccountRepository
import minerva.android.blockchainprovider.repository.smartContract.BlockchainSafeAccountRepositoryImpl
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepositoryImpl
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProvider
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProviderImpl
import org.koin.dsl.module
import java.math.BigInteger

fun createBlockchainProviderModule(
    httpUrls: Map<String, String>,
    ensUrl: String,
    gasPrice: Map<String, BigInteger>,
    wssUrls: Map<String, String>
) = module {
    factory { Web3jProvider.provideWeb3j(httpUrls.toMutableMap(), ensUrl) }
    factory { Web3jProvider.provideEnsResolver(ensUrl) }
    factory<BlockchainRegularAccountRepository> { BlockchainRegularAccountRepositoryImpl(get(), gasPrice, get()) }
    factory<BlockchainSafeAccountRepository> { BlockchainSafeAccountRepositoryImpl(get(), gasPrice) }
    factory<WebSocketServiceProvider> { WebSocketServiceProviderImpl() }
    factory<WebSocketRepository> { WebSocketRepositoryImpl(get(), wssUrls) }
}