package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class RestoreWalletResponse(
    val state: String = String.Empty,
    val message: String = String.Empty
)