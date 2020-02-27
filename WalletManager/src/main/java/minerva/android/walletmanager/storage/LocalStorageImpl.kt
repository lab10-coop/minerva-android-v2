package minerva.android.walletmanager.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.Recipient


class LocalStorageImpl(private val context: Context) : LocalStorage {

    override fun saveIsMnemonicRemembered(isRemembered: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(IS_MNEMONIC_REMEMBERED, isRemembered)
            apply()
        }
    }

    override fun isMnemonicRemembered(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(IS_MNEMONIC_REMEMBERED, false)

    override fun loadRecipients(): List<Recipient> {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(RECIPIENTS, String.NO_DATA).let { raw ->
                return if (raw == String.NO_DATA) listOf()
                else Gson().fromJson(raw, object : TypeToken<List<Recipient>>() {}.type)
            }
    }

    override fun saveRecipient(recipient: Recipient) {
        loadRecipients().toMutableList().let {
            findRecipient(it, recipient).let { index ->
                if(index != Int.InvalidIndex) it[index] = recipient
                else it.add(recipient)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putString(RECIPIENTS, Gson().toJson(it))
                apply()
            }
        }
    }

    private fun findRecipient(list: List<Recipient>, recipient: Recipient): Int {
        list.forEachIndexed { index, it ->
            if (it.address.equals(recipient.address, true)) return index
        }
        return Int.InvalidIndex
    }

    companion object {
        private const val PREFS_NAME = "LocalStorage"
        private const val IS_MNEMONIC_REMEMBERED = "is_mnemonic_remembered"
        private const val RECIPIENTS = "recipients"
    }
}