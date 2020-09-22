package minerva.android.walletmanager.storage

import minerva.android.walletmanager.model.PendingAccount
import minerva.android.walletmanager.model.Recipient

interface LocalStorage {
    fun saveIsMnemonicRemembered(isRemembered: Boolean)
    fun isMnemonicRemembered(): Boolean
    fun getRecipients(): List<Recipient>
    fun saveRecipient(recipient: Recipient)
    fun getProfileImage(name: String): String
    fun saveProfileImage(name: String, image: String)
    fun savePendingAccount(pendingAccount: PendingAccount)
    fun getPendingAccounts(): List<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()
}