package minerva.android.walletmanager.model.defs

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_MAIN_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FOURTH_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_DEFAULT_MAIN_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.THIRD_DEFAULT_NETWORK_INDEX

@SuppressLint("UniqueConstants")
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    DEFAULT_VERSION,
    FIRST_IDENTITY_INDEX,
    FIRST_DEFAULT_NETWORK_INDEX,
    SECOND_DEFAULT_NETWORK_INDEX,
    THIRD_DEFAULT_NETWORK_INDEX,
    FOURTH_DEFAULT_NETWORK_INDEX,
    FIRST_DEFAULT_MAIN_NETWORK_INDEX,
    SECOND_DEFAULT_MAIN_NETWORK_INDEX
)
annotation class DefaultWalletConfigIndexes {
    companion object {
        const val DEFAULT_VERSION = 0
        const val FIRST_IDENTITY_INDEX = 0
        const val FIRST_DEFAULT_NETWORK_INDEX = 0
        const val SECOND_DEFAULT_NETWORK_INDEX = 1
        const val THIRD_DEFAULT_NETWORK_INDEX = 2
        const val FOURTH_DEFAULT_NETWORK_INDEX = 3
        const val FIRST_DEFAULT_MAIN_NETWORK_INDEX = 0
        const val SECOND_DEFAULT_MAIN_NETWORK_INDEX = 1
    }
}