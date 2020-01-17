package minerva.android.walletmanager.model

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.SECOND_VALUES_INDEX

@SuppressLint("UniqueConstants")
@Retention(AnnotationRetention.SOURCE)
@IntDef(DEFAULT_VERSION, FIRST_IDENTITY_INDEX, FIRST_VALUES_INDEX, SECOND_VALUES_INDEX)
annotation class DefaultWalletConfigIndexes {
    companion object {
        const val DEFAULT_VERSION = 0
        const val FIRST_IDENTITY_INDEX = 0
        const val FIRST_VALUES_INDEX = 1
        const val SECOND_VALUES_INDEX = 2
    }
}