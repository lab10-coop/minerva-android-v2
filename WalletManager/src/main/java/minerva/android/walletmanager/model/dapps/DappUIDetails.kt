package minerva.android.walletmanager.model.dapps

data class DappUIDetails(
    val shortName: String,
    val subtitle: String,
    val longName: String,
    val connectLink: String,
    val buttonColor: String,
    val iconLink: String,
    val isSponsored: Boolean,
    val sponsoredOrder: Int,
    val chainIds: List<Int> = emptyList()
)
