package minerva.android.walletmanager.storage

interface LocalStorage {
    fun saveIsMnemonicRemembered(isRemembered: Boolean)
    fun isMnemonicRemembered(): Boolean
}