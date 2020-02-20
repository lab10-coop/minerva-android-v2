package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.IDENTITY
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.SERVICE
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.VALUE

@Retention(AnnotationRetention.SOURCE)
@IntDef(IDENTITY, VALUE, SERVICE)
annotation class WalletActionType {
    companion object {
        const val IDENTITY = 0
        const val VALUE = 1
        const val SERVICE = 2
    }
}