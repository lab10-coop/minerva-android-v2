package minerva.android.widget.repository

import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.NetworkShortName

//TODO downloading network icon need will be refactored
fun getNetworkIcon(network: Network): Int =
    when (network.short) {
        NetworkShortName.ATS_TAU -> R.drawable.ic_artis
        NetworkShortName.POA_SKL -> R.drawable.ic_poa
        NetworkShortName.LUKSO_14 -> R.drawable.ic_lukso
        NetworkShortName.ETH_CLASSIC_KOTTI -> R.drawable.ic_ethereum_classic
        NetworkShortName.ETH_RIN, NetworkShortName.ETH_ROP, NetworkShortName.ETH_GOR, NetworkShortName.ETH_KOV -> R.drawable.ic_ethereum
        else -> Int.InvalidId
    }