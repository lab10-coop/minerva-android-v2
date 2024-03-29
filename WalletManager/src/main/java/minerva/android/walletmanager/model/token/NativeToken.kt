package minerva.android.walletmanager.model.token

data class NativeToken(
    override val chainId: Int,
    override val name: String,
    override val symbol: String,
    val logoRes: Int
) : Token