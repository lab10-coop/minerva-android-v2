package minerva.android.walletmanager.manager

import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.AssetsSet
import minerva.android.walletmanager.model.Network
import java.math.BigDecimal

object AssetManager {

    fun getAssetsFromPairList(network: Network): List<Asset> {
        return when (network) {
            Network.ARTIS -> AssetsSet.artisAssets
            Network.ETHEREUM -> AssetsSet.ethereumAssets
            Network.POA -> AssetsSet.poaAssets
            Network.XDAI -> AssetsSet.xDaiAssets
        }
    }

    fun getAssetAddresses(network: Network): List<String> {
        val assets = getAssetsFromPairList(network)
        val addresses = mutableListOf<String>()
        assets.forEach {
            addresses.add(it.address)
        }
        return addresses
    }

    fun getAssetFromPair(raw: Pair<String, BigDecimal>): Asset {
        val asset = AssetsSet.allAssets.find { it.address == raw.first } ?: Asset(address = raw.first)
        return Asset(asset.name, asset.nameShort, asset.address, raw.second)
    }

    fun getAssetsFromPairList(assetList: List<Pair<String, BigDecimal>>): List<Asset> {
        return assetList.map { getAssetFromPair(it) }
    }
}