package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.ACCOUNT_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.AMOUNT
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.RECEIVER
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.SERVICE_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.CREDENTIAL_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.TOKEN

@Retention(AnnotationRetention.SOURCE)
@StringDef(IDENTITY_NAME, ACCOUNT_NAME, AMOUNT, RECEIVER, SERVICE_NAME, CREDENTIAL_NAME, TOKEN)
annotation class WalletActionFields {
    companion object {
        const val IDENTITY_NAME = "identityName"
        const val ACCOUNT_NAME = "valueName"
        const val AMOUNT = "amount"
        const val RECEIVER = "receiver"
        const val SERVICE_NAME = "service"
        const val CREDENTIAL_NAME = "credentialName"
        const val TOKEN = "token"
    }
}