package minerva.android.walletmanager.model.defs

annotation class PaymentRequest {
    companion object {
        const val CONFIRM_ACTION = "minerva.android.CONFIRM_ACTION"
        const val JWT_TOKEN = "minerva.android.JWT_TOKEN"
        const val AMOUNT = "minerva.android.CP_AMOUNT"
        const val RECIPIENT = "minerva.android.CP_RECIPIENT"
        const val IBAN = "minerva.android.CP_IBAN"
        const val SERVICE_NAME = "minerva.android.CP_SERVICE_NAME"
        const val URL = "minerva.android.CP_SERVICE_URL"
        const val SIGNED_PAYLOAD = "signedPayload"
    }
}