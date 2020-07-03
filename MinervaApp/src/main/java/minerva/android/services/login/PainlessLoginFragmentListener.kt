package minerva.android.services.login

import minerva.android.accounts.listener.BaseScannerListener
import minerva.android.walletmanager.model.QrCodeResponse

interface PainlessLoginFragmentListener: BaseScannerListener {
    fun showChooseIdentityFragment(qrCodeResponse: QrCodeResponse)
}