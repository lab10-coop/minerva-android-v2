package minerva.android.walletmanager.model

import minerva.android.walletmanager.model.defs.NetworkNameFull
import minerva.android.walletmanager.model.defs.NetworkNameShort

enum class Network(val full: String, val short: String) {
    ARTIS(NetworkNameFull.ATS, NetworkNameShort.ATS),
    ETHEREUM(NetworkNameFull.ETH, NetworkNameShort.ETH),
    POA(NetworkNameFull.POA, NetworkNameShort.POA),
    XDAI(NetworkNameFull.XDAI, NetworkNameShort.XDAI);

    companion object {
        private val map = values().associateBy(Network::short)
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}