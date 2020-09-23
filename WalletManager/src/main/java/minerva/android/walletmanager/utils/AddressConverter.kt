package minerva.android.walletmanager.utils

object AddressConverter {

    fun getShortAddress(type: AddressType, address: String) = address.let {
        if (it.length < SHORT_DID_PREFIX_SIZE) it
        else String.format(SHORT_FORMAT, it.substring(START, getPrefixSize(type)), it.substring(it.length - SHORT_SUFFIX_SIZE, it.length))
    }

    private fun getPrefixSize(type: AddressType) =
        when (type) {
            AddressType.NORMAL_ADDRESS -> SHORT_NORMAL_PREFIX_SIZE
            AddressType.DID_ADDRESS -> SHORT_DID_PREFIX_SIZE
        }

    private const val SHORT_FORMAT = "%s...%s"
    private const val START = 0
    private const val SHORT_NORMAL_PREFIX_SIZE = 6
    private const val SHORT_DID_PREFIX_SIZE = 15
    private const val SHORT_SUFFIX_SIZE = 4
}

enum class AddressType {
    NORMAL_ADDRESS, DID_ADDRESS
}