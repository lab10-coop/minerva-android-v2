package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.IDENTITY
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.SERVICE
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.ACCOUNT

@Retention(AnnotationRetention.SOURCE)
@IntDef(IDENTITY, ACCOUNT, SERVICE)
annotation class WalletActionType {
    companion object {
        const val IDENTITY = 0
        const val ACCOUNT = 1
        const val SERVICE = 2
        const val CREDENTIAL = 3
    }
}