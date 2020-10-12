package minerva.android.services.login.uitls

import com.google.firebase.iid.FirebaseInstanceId
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType

object LoginUtils {
    fun isIdentityValid(identity: Identity, requiredData: List<String>): Boolean {
        identity.personalData.let { personalData ->
            requiredData.forEach {
                if (!personalData.containsKey(it)) return false
            }
            return identity.privateKey != String.Empty && identity != IncognitoIdentity()
        }
    }

    fun getService(serviceQrCode: ServiceQrCode, identity: Identity) =
        Service(name = serviceQrCode.serviceName, lastUsed = DateUtils.timestamp, loggedInIdentityPublicKey = identity.publicKey)

    fun getValuesWalletAction(identityName: String, serviceName: String): WalletAction =
        WalletAction(
            WalletActionType.SERVICE, WalletActionStatus.LOG_IN, DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, identityName), Pair(WalletActionFields.SERVICE_NAME, serviceName))
        )

    fun createLoginPayload(identity: Identity, serviceQrCode: ServiceQrCode): Map<String, Map<String, String?>> =
        mutableMapOf(Pair(ChooseIdentityViewModel.PAYLOAD_KEYWORD, createMap(identity, serviceQrCode)))

    private fun createMap(identity: Identity, serviceQrCode: ServiceQrCode): Map<String, String?> =
        mutableMapOf<String, String?>().apply {
            serviceQrCode.requestedData.forEach { identity.personalData[it]?.let { data -> this[it] = data } }
            if (serviceQrCode.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) {
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                    this[ChooseIdentityViewModel.FCM_ID] = result.token
                }
            }
        }

    fun getLoginStatus(serviceQrCode: ServiceQrCode): Int =
        if (serviceQrCode.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) LoginStatus.KNOWN_QUICK_USER
        else LoginStatus.KNOWN_USER
}