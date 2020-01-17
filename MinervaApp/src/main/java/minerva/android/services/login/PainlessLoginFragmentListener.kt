package minerva.android.services.login

import minerva.android.walletmanager.model.QrCodeResponse

interface PainlessLoginFragmentListener {
    fun onBackPressed()
    fun onLoginResult(isLoginSucceed: Boolean)
    fun showChooseIdentityFragment(qrCodeResponse: QrCodeResponse)
}