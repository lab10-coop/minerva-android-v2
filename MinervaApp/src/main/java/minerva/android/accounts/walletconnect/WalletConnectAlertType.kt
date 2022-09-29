package minerva.android.accounts.walletconnect

enum class WalletConnectAlertType {
    NO_ALERT,
    UNDEFINED_NETWORK_WARNING,
    CHANGE_ACCOUNT_WARNING,
    NO_AVAILABLE_ACCOUNT_ERROR,
    UNSUPPORTED_NETWORK_WARNING,
    CHANGE_ACCOUNT //value for calling wallet connection change action
}