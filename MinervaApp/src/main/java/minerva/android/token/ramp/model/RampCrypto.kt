package minerva.android.token.ramp.model

data class RampCrypto(
    val chainId: Int,
    val displaySymbol: String,
    val apiSymbol: String,
    val iconRes: Int,
    val network: String,
    var isSelected: Boolean = false
)