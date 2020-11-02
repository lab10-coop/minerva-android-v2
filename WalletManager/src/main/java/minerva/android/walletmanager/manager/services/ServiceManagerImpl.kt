package minerva.android.walletmanager.manager.services

import androidx.lifecycle.LiveData
import eu.afse.jsonlogic.JsonLogic
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.DateUtils
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.CredentialQrCodeToCredentialMapper
import minerva.android.walletmanager.model.mappers.CredentialRequestMapper
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse
import minerva.android.walletmanager.model.state.ConnectionRequest

class ServiceManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val servicesApi: ServicesApi,
    private val cryptographyRepository: CryptographyRepository
) : ServiceManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun decodeJwtToken(token: String): Single<QrCode> = cryptographyRepository.decodeJwtToken(token)
            .map { mapHashMapToQrCodeResponse(it, token) }

    override fun decodeThirdPartyRequestToken(token: String): Single<ConnectionRequest<Pair<Credential, CredentialRequest>>> =
        cryptographyRepository.decodeJwtToken(token)
            .map { handleCredentialRequest(it) }//todo handle different request types
            .flatMap { updateConnectedService(it) }

    private fun updateConnectedService(it: ConnectionRequest<Pair<Credential, CredentialRequest>>) =
        when (it) {
            is ConnectionRequest.ServiceConnected -> requestedService(it).run { saveService(Service(issuer, name, DateUtils.timestamp, iconUrl.url)).toSingleDefault(it) }
            else -> Single.just(it)
        }

    private fun handleCredentialRequest(map: Map<String, Any?>): ConnectionRequest<Pair<Credential, CredentialRequest>> {
        walletConfigManager.getWalletConfig()?.credentials?.let { credentials ->
            return if (credentials.isEmpty()) {
                ConnectionRequest.VCNotFound
            } else {
                findCredential(map, credentials)
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun findCredential(map: Map<String, Any?>, credentials: List<Credential>): ConnectionRequest<Pair<Credential, CredentialRequest>> {
        CredentialRequestMapper.map(map).apply {
            service.copy(issuer = issuer)
            credentialRequirements?.let {
                credentials.forEach { credential ->
                    if (isRequestedCredentialAvailable(credential, it)) return findRequestedService(this, credential)
                }
            }
        }
        return ConnectionRequest.VCNotFound
    }

    private fun findRequestedService(credentialRequest: CredentialRequest, credential: Credential):
            ConnectionRequest<Pair<Credential, CredentialRequest>> =
        if (isServiceConnected(credentialRequest)) {
            ConnectionRequest.ServiceConnected(Pair(credential, credentialRequest))
        } else {
            ConnectionRequest.ServiceNotConnected(Pair(credential, credentialRequest))
        }

    private fun isServiceConnected(credentialRequest: CredentialRequest) =
        walletConfigManager.getWalletConfig()?.services?.find { it.issuer == credentialRequest.service.issuer } != null

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
                //TODO should be provided the dynamic mechanism for retrieving names and icons from every services
                if (shouldSafeService(identity, service)) saveService(service)
                else Completable.complete()
            }

    private fun shouldSafeService(identity: Identity, service: Service) =
        identity !is IncognitoIdentity && service.name.isNotEmpty() //todo probably different indicator to display service or not will be better

    override fun saveService(service: Service): Completable = walletConfigManager.saveService(service)

    override fun getLoggedInIdentity(publicKey: String): Identity? = walletConfigManager.getLoggedInIdentityByPublicKey(publicKey)

    override fun removeService(issuer: String): Completable {
        walletConfigManager.getWalletConfig()?.apply {
            val newServices = services.toMutableList()
            services.forEach {
                if (it.issuer == issuer) {
                    newServices.remove(it)
                    return walletConfigManager.updateWalletConfig(copy(version = updateVersion, services = newServices))
                }
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun isMoreCredentialToBind(qrCode: CredentialQrCode): Boolean {
        walletConfigManager.getWalletConfig()?.apply {
            return credentials.filter { !filterCredential(it, CredentialQrCodeToCredentialMapper.map(qrCode)) }.size > ONE_ELEMENT
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun updateBindedCredential(qrCode: CredentialQrCode, replace: Boolean): Single<String> {
        walletConfigManager.getWalletConfig()?.apply {
            val updatedCredential = CredentialQrCodeToCredentialMapper.map(qrCode)
            credentials.apply {
                val newCredentials = if (replace) filter { filterCredential(it, updatedCredential) }.toMutableList()
                else toMutableList()
                newCredentials.add(updatedCredential)
                return walletConfigManager.updateWalletConfig(copy(version = updateVersion, credentials = newCredentials))
                    .toSingleDefault(walletConfigManager.findIdentityByDid(qrCode.loggedInDid)?.name)
            }
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun filterCredential(item: Credential, credential: Credential) =
        item.loggedInIdentityDid != credential.loggedInIdentityDid && item.type != credential.type && item.issuer != credential.issuer

    private fun requestedService(serviceConnected: ConnectionRequest.ServiceConnected<Pair<Credential, CredentialRequest>>) =
        serviceConnected.data.second.service

    companion object {
        private const val ONE_ELEMENT = 1
    }
}