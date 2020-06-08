package minerva.android.walletmanager.model

import minerva.android.walletmanager.model.defs.AssetAddress
import minerva.android.walletmanager.model.defs.AssetName
import minerva.android.walletmanager.model.defs.AssetNameShort

object AssetsSet {

    private val FAU = Asset(AssetName.FAU, AssetNameShort.FAU, AssetAddress.FAU)
    private val SFAU = Asset(AssetName.SFAU, AssetNameShort.SFAU, AssetAddress.SFAU)

    val ethereumAssets = listOf(FAU)
    val artisAssets: List<Asset> = listOf(SFAU)
    val poaAssets: List<Asset> = listOf()

    val allAssets: List<Asset>
            get() = artisAssets.union(ethereumAssets).union(poaAssets).toList()
}