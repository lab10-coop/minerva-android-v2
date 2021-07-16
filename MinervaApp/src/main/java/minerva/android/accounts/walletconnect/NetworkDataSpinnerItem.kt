package minerva.android.accounts.walletconnect

data class NetworkDataSpinnerItem(
    val networkName: String,
    val chainId: Int,
    val isAccountAvailable: Boolean = true
)
