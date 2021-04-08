package minerva.android.walletmanager.storage

import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.transactions.Recipient

interface LocalStorage {
    fun saveIsMnemonicRemembered(isRemembered: Boolean)
    fun isMnemonicRemembered(): Boolean

    fun getRecipients(): List<Recipient>
    fun saveRecipient(recipient: Recipient)

    fun getProfileImage(name: String): String
    fun saveProfileImage(name: String, image: String)

    fun getTokenVisibilitySettings(): TokenVisibilitySettings
    fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings

    fun savePendingAccount(pendingAccount: PendingAccount)
    fun getPendingAccounts(): List<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()

    fun loadLastFreeATSTimestamp(): Long
    fun saveFreeATSTimestamp(timestamp: Long)

    fun saveTokenIconsUpdateTimestamp(timestamp: Long)
    fun loadTokenIconsUpdateTimestamp(): Long

    var isBackupAllowed: Boolean
    var isSynced: Boolean
    var areMainNetsEnabled: Boolean
    var isAuthenticationEnabled: Boolean
}