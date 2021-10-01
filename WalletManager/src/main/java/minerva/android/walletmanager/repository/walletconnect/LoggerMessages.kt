package minerva.android.walletmanager.repository.walletconnect

object LoggerMessages {
    const val ON_CONNECTION_OPEN: String = "WC: On connection open: "
    const val ON_SESSION_REQUEST: String = "WC: On session request: "
    const val CONNECTION_TERMINATION: String = "WalletConnect onFailure, connection termination: "
    const val RECONNECTING_CONNECTION: String = "WalletConnect onFailure, reconnecting: "
    const val ON_DISCONNECTING: String = "WC: On disconnect: "
    const val ON_ETH_SIGN: String = "WC: On Eth sign: "
    const val ON_ETH_SEND_TX: String = "WC: On Eth send transaction: "
    const val REJECT_SESSION: String = "WC: Reject session: "
    const val APPROVE_SESSION: String = "WC: Approve session: "
    const val APPROVE_REQUEST: String = "WC: Approve request: "
    const val APPROVE_TX_REQUEST: String = "WC: Approve transaction request: "
    const val REJECT_REQUEST: String = "WC: Reject request: "
    const val KILL_SESSION: String = "WC: Kill session: "
}