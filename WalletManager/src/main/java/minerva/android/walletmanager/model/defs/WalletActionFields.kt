package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.ACCOUNT_NAME

@Retention(AnnotationRetention.SOURCE)
@StringDef(IDENTITY_NAME, ACCOUNT_NAME)
annotation class WalletActionFields {
    companion object {
        const val IDENTITY_NAME = "identityName"
        const val ACCOUNT_NAME = "valueName"
        const val AMOUNT = "amount"
        const val NETWORK = "network"
        const val RECEIVER = "receiver"
        const val SERVICE_NAME = "service"
    }
}