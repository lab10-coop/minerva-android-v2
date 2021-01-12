package minerva.android.walletConnect.model.session

data class WCEncryptionPayload(
    val data: String,
    val hmac: String,
    val iv: String
)
