package minerva.android.walletConnect.model.session

data class WCSessionUpdate(
    val approved: Boolean,
    val chainId: Int?,
    val accounts: List<String>?
)
