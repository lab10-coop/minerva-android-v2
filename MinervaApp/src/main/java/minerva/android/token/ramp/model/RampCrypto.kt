package minerva.android.token.ramp.model

data class RampCrypto(
    val chainId: Int,
    val symbol: String,
    val iconRes: Int,
    val network: String,
    var isSelected: Boolean = false
)