package minerva.android.walletConnect.model.session

import minerva.android.walletConnect.model.enums.MessageType

data class WCSocketMessage(
    val topic: String,
    val type: MessageType,
    val payload: String
)
