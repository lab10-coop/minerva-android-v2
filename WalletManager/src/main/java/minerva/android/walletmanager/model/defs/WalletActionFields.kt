package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.VALUE_NAME

@Retention(AnnotationRetention.SOURCE)
@StringDef(IDENTITY_NAME, VALUE_NAME)
annotation class WalletActionFields {
    companion object {
        const val IDENTITY_NAME = "identityName"
        const val VALUE_NAME = "valueName"
        const val AMOUNT = "amount"
        const val NETWORK = "network"
        const val RECEIVER = "receiver"
        const val SERVICE_NAME = "service"
    }
}