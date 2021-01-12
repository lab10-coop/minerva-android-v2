package minerva.android.walletConnect.model.ethereum

data class WCEthereumSignMessage(
    val raw: List<String>,
    val type: WCSignType
) {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    val data
        get() = when (type) {
            WCSignType.MESSAGE -> raw[1]
            WCSignType.TYPED_MESSAGE -> raw[1]
            WCSignType.PERSONAL_MESSAGE -> raw[0]
        }
}
