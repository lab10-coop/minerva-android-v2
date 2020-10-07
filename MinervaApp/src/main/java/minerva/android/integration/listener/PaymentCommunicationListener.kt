package minerva.android.integration.listener

interface PaymentCommunicationListener {
    fun showConfirmTransactionScreen()
    fun onDeny()
    fun onNewServicesConnected(token: String)
    fun onResultOk(signedData: String)
}