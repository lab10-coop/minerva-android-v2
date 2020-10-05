package minerva.android.walletmanager.manager.services

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.model.state.VCRequestState

interface ServiceManager : Manager {
    fun saveService(service: Service): Completable
    fun decodeJwtToken(token: String): Single<QrCode>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String? = null): Single<String>
    fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable
    fun isAlreadyLoggedIn(issuer: String): Boolean
    fun getLoggedInIdentityPublicKey(issuer: String): String
    fun getLoggedInIdentity(publicKey: String): Identity?
    fun removeService(@ServiceType type: String): Completable
    fun updateBindedCredential(qrCode: CredentialQrCode): Single<String>
    fun decodeThirdPartyRequestToken(token: String): Single<VCRequestState<Pair<Credential, CredentialRequest>>>
}