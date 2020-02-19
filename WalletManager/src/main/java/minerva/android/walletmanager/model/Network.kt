package minerva.android.walletmanager.model

import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.model.defs.NetworkNameFull
import minerva.android.walletmanager.model.defs.NetworkNameShort

enum class Network(val full: String, val short: String, val url: String) {
    ARTIS(NetworkNameFull.ATS, NetworkNameShort.ATS, BuildConfig.ATS_ERC20_ADDRESS),
    ETHEREUM(NetworkNameFull.ETH, NetworkNameShort.ETH, BuildConfig.ETH_ERC20_ADDRESS),
    POA(NetworkNameFull.POA, NetworkNameShort.POA, BuildConfig.POA_ERC20_ADDRESS),
    XDAI(NetworkNameFull.XDAI, NetworkNameShort.XDAI, BuildConfig.XDAI_ERC20_ADDRESS);

    companion object {
        private val map = values().associateBy(Network::short)
        val urlMap = values().associateBy(Network::short).mapValues { it.value.url }
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}