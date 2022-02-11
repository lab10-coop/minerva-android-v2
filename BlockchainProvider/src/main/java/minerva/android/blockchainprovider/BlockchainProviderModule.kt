package minerva.android.blockchainprovider

import minerva.android.blockchainprovider.provider.Web3jProvider
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.ens.ENSRepositoryImpl
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepositoryImpl
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepositoryImpl
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepositoryImpl
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepository
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepositoryImpl
import minerva.android.blockchainprovider.repository.safeAccount.BlockchainSafeAccountRepository
import minerva.android.blockchainprovider.repository.safeAccount.BlockchainSafeAccountRepositoryImpl
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
import minerva.android.blockchainprovider.repository.signature.SignatureRepositoryImpl
import minerva.android.blockchainprovider.repository.superToken.SuperTokenRepository
import minerva.android.blockchainprovider.repository.superToken.SuperTokenRepositoryImpl
import minerva.android.blockchainprovider.repository.transaction.BlockchainTransactionRepository
import minerva.android.blockchainprovider.repository.transaction.BlockchainTransactionRepositoryImpl
import minerva.android.blockchainprovider.repository.units.UnitConverter
import minerva.android.blockchainprovider.repository.units.UnitConverterImpl
import minerva.android.blockchainprovider.repository.validation.ChecksumRepository
import minerva.android.blockchainprovider.repository.validation.ChecksumRepositoryImpl
import minerva.android.blockchainprovider.repository.validation.ValidationRepository
import minerva.android.blockchainprovider.repository.validation.ValidationRepositoryImpl
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepositoryImpl
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProvider
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProviderImpl
import org.koin.dsl.module
import java.math.BigInteger

fun createBlockchainProviderModule(
    httpUrls: Map<Int, String>,
    gasPrice: Map<Int, BigInteger>,
    wssUrls: Map<Int, String>
) = module {
    factory { Web3jProvider.provideWeb3j(httpUrls.toMutableMap(), get()) }
    factory { Web3jProvider.provideEnsResolver(get()) }
    factory<BlockchainSafeAccountRepository> { BlockchainSafeAccountRepositoryImpl(get(), gasPrice) }
    factory<WebSocketServiceProvider> { WebSocketServiceProviderImpl() }
    factory<WebSocketRepository> { WebSocketRepositoryImpl(get(), wssUrls) }
    factory<SignatureRepository> { SignatureRepositoryImpl() }
    factory<SuperTokenRepository> { SuperTokenRepositoryImpl(get(), gasPrice) }
    factory<BlockchainTransactionRepository> { BlockchainTransactionRepositoryImpl(get(), gasPrice, get(), get(), get()) }
    factory<ENSRepository> { ENSRepositoryImpl(get()) }
    factory<ERC20TokenRepository> { ERC20TokenRepositoryImpl(get(), gasPrice) }
    factory<ERC721TokenRepository> { ERC721TokenRepositoryImpl(get(), gasPrice) }
    factory<ERC1155TokenRepository> { ERC1155TokenRepositoryImpl(get(), gasPrice) }
    factory<UnitConverter> { UnitConverterImpl() }
    factory<FreeTokenRepository> { FreeTokenRepositoryImpl() }
    factory<ValidationRepository> { ValidationRepositoryImpl(get()) }
    factory<ChecksumRepository> { ChecksumRepositoryImpl() }
}