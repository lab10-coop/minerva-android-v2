package minerva.android.services.login.uitls

import com.google.firebase.iid.FirebaseInstanceId
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.IdentityField
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

object LoginUtils {
    //TODO change it to dynamic requested fields creation
    fun isIdentityValid(identity: Identity) =
        identity.data[IdentityField.PHONE_NUMBER] != null && identity.data[ChooseIdentityViewModel.NAME] != null

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
            Pair(ChooseIdentityViewModel.PHONE, identity.data[IdentityField.PHONE_NUMBER]),
            Pair(ChooseIdentityViewModel.NAME, identity.data[ChooseIdentityViewModel.NAME]),
            Pair(ChooseIdentityViewModel.IDENTITY_NO, identity.publicKey)
        ).apply {
            if (qrCodeResponse.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) {
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                    this[ChooseIdentityViewModel.FCM_ID] = result.token
                }
            }
        }
}