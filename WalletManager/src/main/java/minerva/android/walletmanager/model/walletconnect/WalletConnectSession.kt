package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletConnect.model.session.WCSession

data class WalletConnectSession(
    val topic: String = String.Empty,
    val version: String = String.Empty,
    val key: String = String.Empty,
    val bridge: String? = String.Empty,
    var relayProtocol: String? = String.Empty,
    var relayData: String? = String.Empty,
    val isMobileWalletConnect: Boolean = false
) {
    fun toUri(): String {
        if (version == WCSession.VERSION_1) {
            return "wc:${topic}@${version}?bridge=${bridge}&key=${key}"
        }
        if (version == WCSession.VERSION_2) {
            var uri = "wc:${topic}@${version}?relay-protocol=${relayProtocol}&symKey=${key}"
            if (relayData != null && relayData != String.Empty) {
                uri += "&relay-data=${relayData}"
            }
            return uri
        }
        return ""
    }
}

