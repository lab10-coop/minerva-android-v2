package minerva.android.walletmanager.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.Recipient


class LocalStorageImpl(private val sharedPreferences: SharedPreferences) : LocalStorage {

    override fun saveIsMnemonicRemembered(isRemembered: Boolean) {
        sharedPreferences.edit().putBoolean(IS_MNEMONIC_REMEMBERED, isRemembered).apply()
    }

    override fun isMnemonicRemembered(): Boolean = sharedPreferences.getBoolean(IS_MNEMONIC_REMEMBERED, false)

    override fun loadRecipients(): List<Recipient> {
        sharedPreferences.getString(RECIPIENTS, String.NO_DATA).let { raw ->
            return if (raw == String.NO_DATA) listOf()
            else Gson().fromJson(raw, object : TypeToken<List<Recipient>>() {}.type)
        }
    }

    override fun saveRecipient(recipient: Recipient) {
        loadRecipients().toMutableList().let {
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

    companion object {
        private const val IS_MNEMONIC_REMEMBERED = "is_mnemonic_remembered"
        private const val RECIPIENTS = "recipients"
    }
}