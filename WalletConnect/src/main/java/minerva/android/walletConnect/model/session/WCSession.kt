package minerva.android.walletConnect.model.session

import android.net.Uri

data class WCSession(
    val topic: String,
    val version: String,
    val key: String,
    val bridge: String?,
    var relayProtocol: String?,
    var relayData: String?,
    val isMobileWalletConnect: Boolean = false
) {
    companion object {
        fun from(from: String): WCSession {
            val uriString = from.replace(WC, WC_URL_PREFIX)
            val uri = Uri.parse(uriString)
            val topic = uri.userInfo
            val version = uri.host
            val key: String?
            var bridge: String? = null
            var relayProtocol: String? = null
            var relayData: String? = null

            when (version) {
                VERSION_1 -> {
                    key = uri.getQueryParameter(KEY)
                    bridge = uri.getQueryParameter(BRIDGE)

                }
                VERSION_2 -> {
                    key = uri.getQueryParameter(SYM_KEY)
                    relayProtocol = uri.getQueryParameter(RELAY_PROTOCOL)
                    relayData = uri.getQueryParameter(RELAY_DATA)
                }
                else -> {
                    throw Throwable(INVALID_WC_ERROR)
                }
            }

            if (topic == null || key == null || (bridge == null && relayProtocol == null)) {
                throw Throwable(INVALID_WC_ERROR)
            }

            return WCSession(topic, version, key, bridge, relayProtocol, relayData)
        }

        private const val BRIDGE = "bridge"
        private const val RELAY_PROTOCOL = "relay-protocol"
        private const val RELAY_DATA = "relay-data"
        private const val KEY = "key"
        private const val SYM_KEY = "symKey"
        private const val WC = "wc:"
        private const val WC_URL_PREFIX = "wc://"
        const val VERSION_1 = "1"
        const val VERSION_2 = "2"
        private const val INVALID_WC_ERROR = "Invalid WalletConnect qr code"
    }
}