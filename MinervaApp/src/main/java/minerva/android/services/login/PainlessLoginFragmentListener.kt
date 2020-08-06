package minerva.android.services.login

import minerva.android.accounts.listener.BaseScannerListener
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.model.ServiceQrResponse

interface PainlessLoginFragmentListener: BaseScannerListener {
    fun showChooseIdentityFragment(qrCodeResponse: ServiceQrResponse)
}