package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.model.defs.NetworkFullName
import minerva.android.walletmanager.model.defs.NetworkShortName

enum class Network(val full: String, val short: String, val url: String) {
    ARTIS(NetworkFullName.ATS, NetworkShortName.ATS, BuildConfig.ATS_ADDRESS),
    ETHEREUM(NetworkFullName.ETH, NetworkShortName.ETH, BuildConfig.ETH_ADDRESS),
    POA(NetworkFullName.POA, NetworkShortName.POA, BuildConfig.POA_ADDRESS),
    // Not available yet
    LUKSO(NetworkFullName.LUKSO, NetworkShortName.LUKSO, String.Empty),
    RSK(NetworkFullName.RSK, NetworkShortName.RSK, String.Empty),
    ETHEREUM_CLASSIC(NetworkFullName.ETH_CLASSIC, NetworkShortName.ETH_CLASSIC, String.Empty);

    companion object {
        private val map = values().associateBy(Network::short)
        val urlMap = values().associateBy(Network::short).mapValues { it.value.url }
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}