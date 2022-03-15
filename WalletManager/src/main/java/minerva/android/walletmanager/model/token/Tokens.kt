package minerva.android.walletmanager.model.token

enum class Tokens(val type: String) {
    ERC_20("ERC-20"),
    ERC_721("ERC-721"),
    ERC_1155("ERC-1155"),
    SUPER_TOKEN("SuperToken")
}