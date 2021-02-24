package minerva.android.integration.listener

import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.minervaprimitives.credential.CredentialRequest

interface PaymentCommunicationListener {
    fun showConfirmTransactionScreen()
    fun onDeny()
    fun onNewServicesConnected(credentialRequest: Pair<Credential, CredentialRequest>)
    fun onResultOk(signedData: String)
}