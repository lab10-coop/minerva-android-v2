package minerva.android.services.login.uitls

import com.google.firebase.iid.FirebaseInstanceId
import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.IdentityField
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.storage.ServiceName
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DateUtils

object LoginUtils {
    //TODO change it to dynamic requested fields creation
    fun isIdentityValid(identity: Identity) =
        identity.personalData[IdentityField.PHONE_NUMBER] != null && identity.personalData[ChooseIdentityViewModel.NAME] != null

    fun getService(qrCodeResponse: QrCodeResponse, identity: Identity) =
        Service(qrCodeResponse.issuer, qrCodeResponse.serviceName, DateUtils.getLastUsedFormatted(), identity.publicKey)

    fun getValuesWalletAction(identityName: String, serviceName: String): WalletAction =
        WalletAction(
            WalletActionType.SERVICE, WalletActionStatus.LOG_IN, DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, identityName), Pair(WalletActionFields.SERVICE_NAME, serviceName))
        )

    //todo change it to dynamic payload creation
    fun createLoginPayload(identity: Identity, qrCodeResponse: QrCodeResponse): Map<String, String?> =
        mutableMapOf(
            Pair(ChooseIdentityViewModel.PHONE, identity.personalData[IdentityField.PHONE_NUMBER]),
            Pair(ChooseIdentityViewModel.NAME, identity.personalData[ChooseIdentityViewModel.NAME]),
            Pair(ChooseIdentityViewModel.IDENTITY_NO, identity.publicKey)
        ).apply {
            if (qrCodeResponse.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) {
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                    this[ChooseIdentityViewModel.FCM_ID] = result.token
                }
            }
        }

    fun getLoginStatus(qrCodeResponse: QrCodeResponse): Int =
        if (qrCodeResponse.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) LoginStatus.KNOWN_QUICK_USER
        else LoginStatus.KNOWN_USER

    fun getRequestedData(requestedData: ArrayList<String>): String {
        var identityFields: String = String.Empty
        requestedData.forEach { identityFields += "$it " }
        return identityFields
    }

    fun getServiceName(response: QrCodeResponse): String =
        when (response.issuer) {
            ServiceType.UNICORN_LOGIN -> ServiceName.UNICORN_LOGIN_NAME
            ServiceType.CHARGING_STATION -> ServiceName.CHARGING_STATION_NAME
            else -> String.Empty
        }
}