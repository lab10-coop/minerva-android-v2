package minerva.android.walletmanager.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.PendingAccount
import minerva.android.walletmanager.model.Recipient

class LocalStorageImpl(private val sharedPreferences: SharedPreferences) : LocalStorage {

    override var isBackupAllowed: Boolean
        set(value) = sharedPreferences.edit().putBoolean(IS_BACKUP_ALLOWED, value).apply()
        get() = sharedPreferences.getBoolean(IS_BACKUP_ALLOWED, true)

    override var isSynced: Boolean
        set(value) = sharedPreferences.edit().putBoolean(IS_SYNCED, value).apply()
        get() = sharedPreferences.getBoolean(IS_SYNCED, true)

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

    override fun getProfileImage(name: String): String = sharedPreferences.getString(name, String.NO_DATA) ?: String.NO_DATA

    override fun saveProfileImage(name: String, image: String) = sharedPreferences.edit().putString(name, image).apply()

    companion object {
        private const val IS_MNEMONIC_REMEMBERED = "is_mnemonic_remembered"
        private const val IS_BACKUP_ALLOWED = "is_mnemonic_remembered"
        private const val IS_SYNCED = "is_synced"
        private const val RECIPIENTS = "recipients"
        private const val PENDING_ACCOUNTS = "pending_accounts"
    }
}