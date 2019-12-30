package minerva.android.walletmanager.storage

import android.content.Context

class LocalStorage(private val context: Context) {

    fun saveIsMnemonicRemembered(isRemembered: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(IS_MNEMONIC_REMEMBERED, isRemembered)
            apply()
        }
    }

    fun isMenmonicRemembered(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(IS_MNEMONIC_REMEMBERED, false)

    companion object {
        private const val PREFS_NAME = "LocalStorage"
        private const val IS_MNEMONIC_REMEMBERED = "is_mnemonic_remembered"
    }
}