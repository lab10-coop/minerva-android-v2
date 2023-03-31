package minerva.android.walletConnect.model.ethereum

data class WCEthereumSignMessage(
    val raw: List<String>,
    val type: WCSignType
) {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    val address
        get() = when (type) {
            WCSignType.MESSAGE -> raw[FIRST]
            WCSignType.TYPED_MESSAGE -> raw[FIRST]
            WCSignType.PERSONAL_MESSAGE -> raw[SECOND]
        }

    val data
        get() = when (type) {
            WCSignType.MESSAGE -> raw[SECOND]
            WCSignType.TYPED_MESSAGE -> raw[SECOND]
            WCSignType.PERSONAL_MESSAGE -> raw[FIRST]
        }

    companion object {
        private const val FIRST = 0
        private const val SECOND = 1
    }
}
