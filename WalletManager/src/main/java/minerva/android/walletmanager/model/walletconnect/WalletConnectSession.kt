package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty

data class WalletConnectSession(
    val topic: String = String.Empty,
    val version: String = String.Empty,
    val key: String = String.Empty,
    val bridge: String? = String.Empty,
    var relayProtocol: String? = String.Empty,
    var relayData: String? = String.Empty,
) {
    fun toUri(): String {
        if (version == "1") {
            return "wc:${topic}@${version}?bridge=${bridge}&key=${key}"
        }
        if (version == "2") {
            var uri = "wc:${topic}@${version}?relay-protocol=${relayProtocol}&symKey=${key}"
            if (relayData != null && relayData != String.Empty) {
                uri += "&relay-data=${relayData}"
            }
            return uri
        }
        return ""
    }
}

