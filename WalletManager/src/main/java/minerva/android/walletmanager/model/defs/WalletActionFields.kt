package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.INDENTITY_NAME

@Retention(AnnotationRetention.SOURCE)
@StringDef(INDENTITY_NAME)
annotation class WalletActionFields {
    companion object {
        const val INDENTITY_NAME = "identityName"
    }
}