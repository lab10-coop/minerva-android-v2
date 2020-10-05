package minerva.android.integration.listener

interface PaymentCommunicationListener {
    fun showConfirmTransactionScreen()
    fun onDeny()
    fun onNewServicesConnected()
    fun onResultOk(signedData: String)
}