package minerva.android.walletmanager.model

object AssetsSet {
    //TODO implement it for production URL
    val artisAssets: List<Asset> = listOf()

    val ethereumAssets: List<Asset> = listOf()

    val poaAssets: List<Asset> = listOf()

    val allAssets: List<Asset>
        get() = artisAssets.union(ethereumAssets).union(poaAssets).toList()
}