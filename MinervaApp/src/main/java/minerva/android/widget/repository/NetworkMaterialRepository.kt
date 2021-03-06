package minerva.android.widget.repository

import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.AssetName

fun getNetworkColor(network: Network, opacity: Boolean = false): Int {
    val position = when (network) {
        Network.ARTIS -> ARTIS_ARRAY_POSITION
        Network.ETHEREUM -> ETHEREUM_ARRAY_POSITION
        Network.POA -> POA_ARRAY_POSITION
        else -> TODO("Implement the rest of colors for networks")
    }
    return if (opacity) networkOpacityColor[position]
    else networkColor[position]
}

fun getNetworkIcon(network: Network): Int =
    when (network) {
        Network.ARTIS -> R.drawable.ic_artis
        Network.ETHEREUM -> R.drawable.ic_ethereum
        Network.POA -> R.drawable.ic_poa
        Network.LUKSO -> R.drawable.ic_lukso
        Network.RSK -> R.drawable.ic_rsk
        Network.ETHEREUM_CLASSIC -> R.drawable.ic_ethereum_classic

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