package minerva.android.widget

import minerva.android.R
import minerva.android.walletmanager.walletconfig.Network
import kotlin.math.abs

fun generateColor(value: String, opacity: Boolean = false): Int {
    val index = abs(value.hashCode() % color.size)
    return if (opacity) opacityColor[index]
    else color[index]
}

fun getNetworkColor(network: Network, opacity: Boolean = false): Int {
    val position = when (network) {
        Network.ARTIS -> ARTIS_ARRAY_POSITION
        Network.ETHEREUM -> ETHEREUM_ARRAY_POSITION
        Network.POA -> POA_ARRAY_POSITION
        Network.XDAI -> XDAI_ARRAY_POSITION
    }
    return if (opacity) networkOpacityColor[position]
    else networkColor[position]
}

fun getNetworkIcon(network: Network): Int =
    when (network) {
        Network.ARTIS -> R.drawable.ic_artis
        Network.ETHEREUM -> R.drawable.ic_ethereum
        Network.POA -> R.drawable.ic_poa
        Network.XDAI -> R.drawable.ic_xdai
    }

private val color = listOf(
    R.color.colorSetOne,
    R.color.colorSetTwo,
    R.color.colorSetThree,
    R.color.colorSetFour,
    R.color.colorSetFive,
    R.color.colorSetSix,
    R.color.colorSetSeven,
    R.color.colorSetEight,
    R.color.colorSetNine,
    R.color.colorSetTen
)

private val opacityColor = listOf(
    R.color.colorSetOneOpacity,
    R.color.colorSetTwoOpacity,
    R.color.colorSetThreeOpacity,
    R.color.colorSetFourOpacity,
    R.color.colorSetFiveOpacity,
    R.color.colorSetSixOpacity,
    R.color.colorSetSevenOpacity,
    R.color.colorSetEightOpacity,
    R.color.colorSetNineOpacity,
    R.color.colorSetTenOpacity
)

private val networkColor = listOf(
    R.color.artis,
    R.color.ethereum,
    R.color.poa,
    R.color.xDai
)

private val networkOpacityColor = listOf(
    R.color.artisOpacity,
    R.color.ethereumOpacity,
    R.color.poaOpacity,
    R.color.xDaiOpacity
)

private const val ARTIS_ARRAY_POSITION = 0
private const val ETHEREUM_ARRAY_POSITION = 1
private const val POA_ARRAY_POSITION = 2
private const val XDAI_ARRAY_POSITION = 3