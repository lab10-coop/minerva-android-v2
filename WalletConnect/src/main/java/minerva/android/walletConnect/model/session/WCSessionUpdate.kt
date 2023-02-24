package minerva.android.walletConnect.model.session

data class WCSessionUpdate(
    val approved: Boolean,
    val chainId: Int?,
    val accounts: List<String>?
)

/**
 * WC Switch Ethereum Chain - data class for storing data related to chain we try to switch
 * @param chainId - chain id which we try to set
 */
data class WCSwitchEthereumChain(val chainId: String?)