package minerva.android.services.login

import minerva.android.accounts.listener.BaseScannerListener
import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode

interface ServicesScannerListener : BaseScannerListener {
    fun onPainlessLoginResult(isLoginSucceed: Boolean, payload: LoginPayload? = null)
    fun showChooseIdentityFragment(qrCode: ServiceQrCode)
    fun updateBindedCredential(qrCode: CredentialQrCode)
    fun onScannerResult(isResultSucceed: Boolean, message: String? = String.Empty)
}