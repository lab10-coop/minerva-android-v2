package minerva.android.walletConnect.model.session

import android.net.Uri

data class WCSession(
    val topic: String,
    val version: String,
    val bridge: String,
    val key: String
) {
    companion object {
        fun from(from: String): WCSession {
            val uriString = from.replace(WC, WC_URL_PREFIX)
            val uri = Uri.parse(uriString)
            val bridge = uri.getQueryParameter(BRIDGE)
            val key = uri.getQueryParameter(KEY)
            val topic = uri.userInfo
            val version = uri.host

            if (bridge == null || key == null || topic == null || version == null) {
                throw Throwable("Invalid WalletConnect qr code")
            }

            return WCSession(topic, version, bridge, key)
        }

        private const val BRIDGE = "bridge"
        private const val KEY = "key"
        private const val WC = "wc:"
        private const val WC_URL_PREFIX = "wc://"
    }
}