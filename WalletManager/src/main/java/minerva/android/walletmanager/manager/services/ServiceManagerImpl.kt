package minerva.android.walletmanager.manager.services

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.CredentialQrCodeToCredentialMapper
import minerva.android.walletmanager.model.mappers.PaymentMapper
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse

class ServiceManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val servicesApi: ServicesApi,
    private val cryptographyRepository: CryptographyRepository
) : ServiceManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun decodeJwtToken(token: String): Single<QrCode> =
        cryptographyRepository.decodeJwtToken(token)
            .map { mapHashMapToQrCodeResponse(it) }

    override fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>> =
        cryptographyRepository.decodeJwtToken(token)
            .map { PaymentMapper.map(it) }
            .map { Pair(it, walletConfigManager.getWalletConfig()?.services) }

    override fun createJwtToken(payload: Map<String, Any?>, privateKey: String?): Single<String> =
        cryptographyRepository.createJwtToken(payload, privateKey ?: walletConfigManager.masterSeed.privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable {
                //TODO uncomment this after server side service object type refactor
                //if (identity !is IncognitoIdentity) saveService(service)
                //else Completable.complete()
                Completable.complete()
            }

    override fun saveService(service: Service): Completable = walletConfigManager.saveService(service)

    override fun isAlreadyLoggedIn(issuer: String): Boolean = walletConfigManager.isAlreadyLoggedIn(issuer)

    override fun getLoggedInIdentityPublicKey(issuer: String): String = walletConfigManager.getLoggedInIdentityPublicKey(issuer)

    override fun getLoggedInIdentity(publicKey: String): Identity? = walletConfigManager.getLoggedInIdentityByPublicKey(publicKey)

    override fun removeService(type: String): Completable {
        walletConfigManager.getWalletConfig()?.apply {
            val newServices = services.toMutableList()
            services.forEach {
                if (it.type == type) {
                    newServices.remove(it)
                    return walletConfigManager.updateWalletConfig(WalletConfig(updateVersion, identities, accounts, newServices, credentials))
                }
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun updateBindedCredential(qrCode: CredentialQrCode): Single<String> {
        walletConfigManager.getWalletConfig()?.apply {
            val updatedCredential = CredentialQrCodeToCredentialMapper.map(qrCode)
            val newCredentials = credentials.toMutableList().apply {
                this[getPositionForCredential(updatedCredential)] = updatedCredential
            }
            return walletConfigManager.updateWalletConfig(WalletConfig(updateVersion, identities, accounts, services, newCredentials))
                .toSingleDefault(walletConfigManager.findIdentityByDid(qrCode.loggedInDid)?.name)
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun WalletConfig.getPositionForCredential(credential: Credential): Int {
        credentials.forEachIndexed { position, item ->
            if (item.loggedInIdentityDid == credential.loggedInIdentityDid && item.type == credential.type && item.issuer == credential.issuer) return position
        }
        return identities.size
    }
}