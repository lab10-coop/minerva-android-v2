package minerva.android.walletmanager.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.TokenVisibilitySettings
import minerva.android.walletmanager.model.PendingAccount
import minerva.android.walletmanager.model.Recipient

//TODO move saving object to Room - MinervaDatabase, handle primitives only here
class LocalStorageImpl(private val sharedPreferences: SharedPreferences) : LocalStorage {

    override var isBackupAllowed: Boolean
        set(value) = sharedPreferences.edit().putBoolean(IS_BACKUP_ALLOWED, value).apply()
        get() = sharedPreferences.getBoolean(IS_BACKUP_ALLOWED, true)

    override var isSynced: Boolean
        set(value) = sharedPreferences.edit().putBoolean(IS_SYNCED, value).apply()
        get() = sharedPreferences.getBoolean(IS_SYNCED, true)

    override var areMainNetsEnabled: Boolean
        set(value) = sharedPreferences.edit().putBoolean(ARE_MAIN_NETS_ENABLED, value).apply()
        get() = sharedPreferences.getBoolean(ARE_MAIN_NETS_ENABLED, false)

    override fun saveIsMnemonicRemembered(isRemembered: Boolean) {
        sharedPreferences.edit().putBoolean(IS_MNEMONIC_REMEMBERED, isRemembered).apply()
    }

    override fun isMnemonicRemembered(): Boolean = sharedPreferences.getBoolean(IS_MNEMONIC_REMEMBERED, false)

    override fun getRecipients(): List<Recipient> {
        sharedPreferences.getString(RECIPIENTS, String.NO_DATA).let { raw ->
            return if (raw == String.NO_DATA) listOf()
            else Gson().fromJson(raw, object : TypeToken<List<Recipient>>() {}.type)
        }
    }

    override fun saveRecipient(recipient: Recipient) {
        getRecipients().toMutableList().let {
            findRecipient(it, recipient).let { index ->
                if (index != Int.InvalidIndex) it[index] = recipient
                else it.add(recipient)
            }
            sharedPreferences.edit().putString(RECIPIENTS, Gson().toJson(it)).apply()
        }
    }

    private fun findRecipient(list: List<Recipient>, recipient: Recipient): Int {
        list.forEachIndexed { index, it ->
            if (it.address.equals(recipient.address, true)) return index
        }
        return Int.InvalidIndex
    }

    override fun savePendingAccount(pendingAccount: PendingAccount) {
        getPendingAccounts().toMutableList().let {
            it.add(pendingAccount)
            sharedPreferences.edit().putString(PENDING_ACCOUNTS, Gson().toJson(it)).apply()
        }
    }

    override fun getPendingAccounts(): List<PendingAccount> {
        sharedPreferences.getString(PENDING_ACCOUNTS, String.NO_DATA).let { raw ->
            return if (raw == String.NO_DATA) listOf()
            else Gson().fromJson(raw, object : TypeToken<List<PendingAccount>>() {}.type)
        }
    }

    override fun removePendingAccount(pendingAccount: PendingAccount) {
        getPendingAccounts().toMutableList().let { list ->
            list.removeAt(list.indexOf(pendingAccount))
            sharedPreferences.edit().putString(PENDING_ACCOUNTS, Gson().toJson(list)).apply()
        }
    }

    override fun clearPendingAccounts() {
        sharedPreferences.edit().remove(PENDING_ACCOUNTS).apply()
    }

    override fun loadLastFreeATSTimestamp(): Long = sharedPreferences.getLong(FREE_ATS_TIMESTAMP, Long.InvalidValue)

    override fun saveFreeATSTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong(FREE_ATS_TIMESTAMP, timestamp).apply()
    }

    override fun saveTokenIconsUpdateTimestamp(timestamp: Long) =
        sharedPreferences.edit().putLong(ICON_UPDATE_TIMESTAMP, timestamp).apply()

    override fun loadTokenIconsUpdateTimestamp(): Long = sharedPreferences.getLong(ICON_UPDATE_TIMESTAMP, Long.InvalidValue)

    override fun getProfileImage(name: String): String = sharedPreferences.getString(name, String.NO_DATA) ?: String.NO_DATA

    override fun saveProfileImage(name: String, image: String) = sharedPreferences.edit().putString(name, image).apply()

    override fun getTokenVisibilitySettings(): TokenVisibilitySettings =
        sharedPreferences.getString(ASSET_VISIBILITY_SETTINGS, String.NO_DATA).let { raw ->
            if (raw == String.NO_DATA) TokenVisibilitySettings()
            else Gson().fromJson(raw, object : TypeToken<TokenVisibilitySettings>() {}.type)
        }

    override fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings {
        sharedPreferences.edit().putString(ASSET_VISIBILITY_SETTINGS, Gson().toJson(settings)).apply()
        return settings
    }

    companion object {
        private const val IS_MNEMONIC_REMEMBERED = "is_mnemonic_remembered"
        private const val IS_BACKUP_ALLOWED = "is_mnemonic_remembered"
        private const val ARE_MAIN_NETS_ENABLED = "are_main_nets_enabled"
        private const val IS_SYNCED = "is_synced"
        private const val RECIPIENTS = "recipients"
        private const val PENDING_ACCOUNTS = "pending_accounts"
        private const val ASSET_VISIBILITY_SETTINGS = "asset_visibility_settings"
        private const val FREE_ATS_TIMESTAMP = "free_ats_timestamp"
        private const val ICON_UPDATE_TIMESTAMP = "last_update_timestamp"
    }
}