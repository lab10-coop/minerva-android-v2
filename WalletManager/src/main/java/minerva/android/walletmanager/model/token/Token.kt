package minerva.android.walletmanager.model.token

interface Token {
    val chainId: Int
    val name: String
    val symbol: String
}