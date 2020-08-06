package minerva.android.walletmanager.manager.services

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.function.orElse
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.PaymentMapper
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.utils.IdentityUtils

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

    override fun getLoggedInIdentity(publicKey: String): Identity? = walletConfigManager.getLoggedInIdentityByPyblicKey(publicKey)

    override fun removeService(type: String): Completable {
        walletConfigManager.getWalletConfig()?.apply {
            val newServices = services.toMutableList()
            services.forEach {
                if (it.type == type) {
                    newServices.remove(it)
                    return walletConfigManager.updateWalletConfig(WalletConfig(version, identities, accounts, newServices))
                }
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun bindCredentialToIdentity(newCredential: Credential): Single<String> {
        walletConfigManager.getWalletConfig()?.apply {
            identities.filter { !it.isDeleted }.forEach { identity ->
                if (isLoggedInCredential(identity, newCredential)) return bindCredential(identity, newCredential)
            }
            return Single.error(NoBindedCredentialThrowable())
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun isLoggedInCredential(identity: Identity, newCredential: Credential) = identity.did == newCredential.loggedInIdentityDid

    private fun WalletConfig.bindCredential(identity: Identity, newCredential: Credential): Single<String> {
        getBindedCredential(identity, newCredential)?.let { credential ->
            updateCredential(identity, credential)
            return updateWalletConfig(identity, this)
        }.orElse {
            return updateWalletConfig(getIdentityWithNewCredential(identity, newCredential), this)
        }
    }

    private fun getBindedCredential(identity: Identity, credential: Credential): Credential? =
        identity.credentials.find { item -> item.issuer == credential.issuer && item.type == credential.type }

    private fun updateCredential(identity: Identity, credential: Credential) {
        identity.credentials.find { found -> found == credential }?.lastUsed = DateUtils.getDateWithTimeFromTimestamp()
    }

    private fun getIdentityWithNewCredential(identity: Identity, newCredential: Credential): Identity =
        identity.apply { credentials = credentials + newCredential }

    private fun updateWalletConfig(identity: Identity, walletConfig: WalletConfig): Single<String> =
        walletConfigManager.updateWalletConfig(
            WalletConfig(
                walletConfig.version,
                IdentityUtils.prepareIdentities(identity, walletConfig),
                walletConfig.accounts,
                walletConfig.services
            )
        ).toSingleDefault(identity.name)

}