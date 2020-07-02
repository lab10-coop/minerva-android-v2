package minerva.android.walletmanager.manager.services

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Payment
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.model.Service

interface ServiceManager : Manager {
    fun saveService(service: Service): Completable
    fun decodeQrCodeResponse(token: String): Single<QrCodeResponse>
    fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>>
    fun createJwtToken(payload: Map<String, Any?>): Single<String>
    fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable
    fun isAlreadyLoggedIn(issuer: String): Boolean
    fun getLoggedInIdentityPublicKey(issuer: String): String
    fun getLoggedInIdentity(publicKey: String): Identity?
}