package minerva.android.walletmanager.storage

import minerva.android.walletmanager.model.Recipient

interface LocalStorage {
    fun saveIsMnemonicRemembered(isRemembered: Boolean)
    fun isMnemonicRemembered(): Boolean
    fun loadRecipients(): List<Recipient>
    fun saveRecipient(recipient: Recipient)
}