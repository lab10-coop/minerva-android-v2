package minerva.android.walletmanager.model.defs

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_ACCOUNTS_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_NETWORK
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_ACCOUNTS_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_NETWORK

@SuppressLint("UniqueConstants")
@Retention(AnnotationRetention.SOURCE)
@IntDef(DEFAULT_VERSION, FIRST_IDENTITY_INDEX, FIRST_ACCOUNTS_INDEX, SECOND_ACCOUNTS_INDEX, FIRST_NETWORK, SECOND_NETWORK)
annotation class DefaultWalletConfigIndexes {
    companion object {
        const val DEFAULT_VERSION = 0
        const val FIRST_IDENTITY_INDEX = 0
        const val FIRST_ACCOUNTS_INDEX = 0
        const val SECOND_ACCOUNTS_INDEX = 1
        const val FIRST_NETWORK = 0
        const val SECOND_NETWORK = 1
    }
}