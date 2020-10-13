package minerva.android.walletmanager.manager.services

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.state.ConnectionRequest

interface ServiceManager : Manager {
    fun saveService(service: Service): Completable
    fun decodeJwtToken(token: String): Single<QrCode>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String? = null): Single<String>
    fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable
    fun getLoggedInIdentity(publicKey: String): Identity?
    fun removeService(issuer: String): Completable
    fun updateBindedCredential(qrCode: CredentialQrCode, replace: Boolean): Single<String>
    fun isMoreCredentialToBind(qrCode: CredentialQrCode): Boolean
    fun decodeThirdPartyRequestToken(token: String): Single<ConnectionRequest<Pair<Credential, CredentialRequest>>>
}