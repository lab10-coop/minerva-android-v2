package minerva.android.accounts.transaction.fragment.scanner

object AddressParser {

    fun parse(address: String): String {
        val parts = address.split(META_ADDRESS_SEPARATOR)
        return when {
            parts.first() == WALLET_CONNECT -> parts.first()
            parts.size > TWO_ELEMENT_SIZE -> address
            else -> parts.last()
        }
    }

    private const val META_ADDRESS_SEPARATOR = ":"
    private const val TWO_ELEMENT_SIZE = 2
    const val WALLET_CONNECT = "wc"
}