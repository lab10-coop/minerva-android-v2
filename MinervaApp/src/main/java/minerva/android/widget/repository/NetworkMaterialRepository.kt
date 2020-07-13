package minerva.android.widget.repository

import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.AssetName

fun getNetworkColor(network: Network, opacity: Boolean = false): Int {
    val position = when (network) {
        Network.ATS_TAU -> ARTIS_ARRAY_POSITION
        Network.ETH_RIN -> ETHEREUM_ARRAY_POSITION
        Network.POA_SKL -> POA_ARRAY_POSITION
        else -> TODO("Implement the rest of colors for networks")
    }
    return if (opacity) networkOpacityColor[position]
    else networkColor[position]
}

fun getNetworkIcon(network: Network): Int =
    when (network) {
        Network.ATS_TAU -> R.drawable.ic_artis
        Network.ETH_RIN -> R.drawable.ic_ethereum
        Network.POA_SKL -> R.drawable.ic_poa
        Network.LUKSO_14 -> R.drawable.ic_lukso
        Network.RSK_TRSK -> R.drawable.ic_rsk
        Network.ETH_CLASSIC_KOTTI -> R.drawable.ic_ethereum_classic
        Network.ETH_ROP, Network.ETH_GOR, Network.ETH_KOV -> R.drawable.ic_ethereum
    }

//TODO add missing asset Icons
fun getAssetIcon(asset: Asset): Int =
    when (asset.name) {
        AssetName.SSAI -> Int.InvalidId
        AssetName.SAI -> R.drawable.ic_dai_sai
        AssetName.DAI -> R.drawable.ic_dai_sai
        AssetName.ATS20 -> Int.InvalidId
        AssetName.SFAU -> Int.InvalidId
        AssetName.FAU -> Int.InvalidId
        else -> Int.InvalidId
    }

private val networkColor = listOf(
    R.color.artis,
    R.color.ethereum,
    R.color.poa
)

private val networkOpacityColor = listOf(
    R.color.artisOpacity,
    R.color.ethereumOpacity,
    R.color.poaOpacity
)

private const val ARTIS_ARRAY_POSITION = 0
private const val ETHEREUM_ARRAY_POSITION = 1
private const val POA_ARRAY_POSITION = 2