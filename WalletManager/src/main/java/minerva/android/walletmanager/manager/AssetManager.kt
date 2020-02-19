package minerva.android.walletmanager.manager

import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.AssetsSet
import minerva.android.walletmanager.model.Network
import java.math.BigDecimal

object AssetManager {

    fun getAssets(network: Network): List<Asset> =
        when (network) {
            Network.ARTIS -> AssetsSet.artisAssets
            Network.ETHEREUM -> AssetsSet.ethereumAssets
            Network.POA -> AssetsSet.poaAssets
            Network.XDAI -> AssetsSet.xDaiAssets
        }

    fun getAssetAddresses(network: Network): Pair<String, List<String>> =
        Pair(network.short, getAssets(network).map { it.address })

    fun getAssetFromPair(raw: Pair<String, BigDecimal>): Asset {
        val asset = AssetsSet.allAssets.find { it.address == raw.first } ?: Asset(address = raw.first)
        return Asset(asset.name, asset.nameShort, asset.address, raw.second)
    }

    fun mapToAssets(assetList: List<Pair<String, BigDecimal>>): List<Asset> = assetList.map { getAssetFromPair(it) }
}