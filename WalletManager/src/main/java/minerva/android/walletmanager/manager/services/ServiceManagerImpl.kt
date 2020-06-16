package minerva.android.walletmanager.manager.services

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.PaymentMapper
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse

class ServiceManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val servicesApi: ServicesApi,
    private val cryptographyRepository: CryptographyRepository
) : ServiceManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun decodeQrCodeResponse(token: String): Single<QrCodeResponse> =
        cryptographyRepository.decodeJwtToken(token)
            .map { mapHashMapToQrCodeResponse(it) }

    override fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>> =
        cryptographyRepository.decodeJwtToken(token)
            .map { PaymentMapper.map(it) }
            .map { Pair(it, walletConfigManager.getWalletConfig()?.services) }

    override fun createJwtToken(payload: Map<String, Any?>): Single<String> =
        cryptographyRepository.createJwtToken(payload, walletConfigManager.masterSeed.privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable {
                if (identity !is IncognitoIdentity) saveService(service)
                else Completable.complete()
            }

    override fun saveService(service: Service): Completable = walletConfigManager.saveService(service)

    override fun isAlreadyLoggedIn(issuer: String): Boolean = walletConfigManager.isAlreadyLoggedIn(issuer)

    override fun getLoggedInIdentityPublicKey(issuer: String): String = walletConfigManager.getLoggedInIdentityPublicKey(issuer)

    override fun getLoggedInIdentity(publicKey: String): Identity? = walletConfigManager.getLoggedInIdentity(publicKey)
}