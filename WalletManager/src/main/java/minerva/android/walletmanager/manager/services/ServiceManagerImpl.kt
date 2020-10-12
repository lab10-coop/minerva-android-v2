package minerva.android.walletmanager.manager.services

import androidx.lifecycle.LiveData
import eu.afse.jsonlogic.JsonLogic
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.CredentialQrCodeToCredentialMapper
import minerva.android.walletmanager.model.mappers.CredentialRequestMapper
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse
import minerva.android.walletmanager.model.state.VCRequestState

class ServiceManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val servicesApi: ServicesApi,
    private val cryptographyRepository: CryptographyRepository
) : ServiceManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun decodeJwtToken(token: String): Single<QrCode> =
        cryptographyRepository.decodeJwtToken(token)
            .map { mapHashMapToQrCodeResponse(it, token) }

    override fun decodeThirdPartyRequestToken(token: String): Single<VCRequestState<Pair<Credential, CredentialRequest>>> =
        cryptographyRepository.decodeJwtToken(token)
            .map { handleCredentialRequest(it) }//todo handle different request types

    private fun handleCredentialRequest(map: Map<String, Any?>): VCRequestState<Pair<Credential, CredentialRequest>> {
        walletConfigManager.getWalletConfig()?.credentials?.let { credentials ->
            if (credentials.isEmpty()) {
                return VCRequestState.NotFound
            } else {
                val credentialRequest = CredentialRequestMapper.map(map)
                credentialRequest.credentialRequirements?.let {
                    credentials.forEach { credential ->
                        if (isRequestedCredentialAvailable(credential, it)) {
                            return VCRequestState.Found(Pair(credential, credentialRequest))
                        }
                    }
                }
            }
        }
        return VCRequestState.NotFound
    }

    private fun isRequestedCredentialAvailable(credential: Credential, requirements: CredentialRequirements) =
        isIssValid(requirements, credential.issuer) &&
                isRequestedType(requirements, credential.type) &&
                isRequestedType(requirements, credential.membershipType)

    private fun isRequestedType(requirements: CredentialRequirements, type: String): Boolean =
        requirements.type.find { it == type } != null

    private fun isIssValid(credentialRequest: CredentialRequirements, iss: String): Boolean =
        if (credentialRequest.constraints.isEmpty()) false
        else JsonLogic().apply(credentialRequest.constraints[0], iss).toBoolean() //JsonLogic in constraints is check against the iss

    override fun createJwtToken(payload: Map<String, Any?>, privateKey: String?): Single<String> =
        cryptographyRepository.createJwtToken(payload, privateKey ?: walletConfigManager.masterSeed.privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable {
                //TODO should be provided the dynamic mechanism for retrieving names and icons from services
                if (shouldSafeService(identity, service)) saveService(service)
                else Completable.complete()
            }

    private fun shouldSafeService(identity: Identity, service: Service) =
        identity !is IncognitoIdentity && service.name.isNotEmpty()

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