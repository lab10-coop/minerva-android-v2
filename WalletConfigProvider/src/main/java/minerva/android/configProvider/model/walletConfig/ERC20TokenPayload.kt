package minerva.android.configProvider.model.walletConfig

import minerva.android.kotlinUtils.Empty

data class ERC20TokenPayload(
    val name: String = String.Empty,
    val symbol: String = String.Empty,
    val address: String = String.Empty,
    val decimals: String = String.Empty
)