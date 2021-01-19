package minerva.android.walletConnect.model.session

data class WCApproveSessionResponse(
    val approved: Boolean = true,
    val chainId: Int,
    val accounts: List<String>,
    val peerId: String?,
    val peerMeta: WCPeerMeta?
)
