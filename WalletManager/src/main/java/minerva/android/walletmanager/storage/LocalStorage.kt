package minerva.android.walletmanager.storage

import minerva.android.walletmanager.model.TokenVisibilitySettings
import minerva.android.walletmanager.model.PendingAccount
import minerva.android.walletmanager.model.Recipient

interface LocalStorage {
    fun saveIsMnemonicRemembered(isRemembered: Boolean)
    fun isMnemonicRemembered(): Boolean

    fun getRecipients(): List<Recipient>
    fun saveRecipient(recipient: Recipient)

    fun getProfileImage(name: String): String
    fun saveProfileImage(name: String, image: String)

    fun getAssetVisibilitySettings(): TokenVisibilitySettings
    fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings

    fun savePendingAccount(pendingAccount: PendingAccount)
    fun getPendingAccounts(): List<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()

    fun getLastFreeATSTimestamp(): Long
    fun saveFreeATSTimestamp(timestamp: Long)

    var isBackupAllowed: Boolean
    var isSynced: Boolean
    var areMainNetsEnabled: Boolean
}