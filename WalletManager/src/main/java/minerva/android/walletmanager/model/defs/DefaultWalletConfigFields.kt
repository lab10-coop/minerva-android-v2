package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_IDENTITY
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PHONE
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PRIVATE_KEY
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PUBLIC_KEY

@Retention(AnnotationRetention.SOURCE)
@StringDef(DEFAULT_IDENTITY_NAME, INCOGNITO_IDENTITY, INCOGNITO_NAME, INCOGNITO_PHONE, INCOGNITO_PRIVATE_KEY, INCOGNITO_PUBLIC_KEY)
annotation class DefaultWalletConfigFields {
    companion object {
        const val DEFAULT_IDENTITY_NAME = "Identity #1"
        const val INCOGNITO_IDENTITY = "Incognito Identity"
        const val INCOGNITO_NAME = "Incognito mode"
        const val INCOGNITO_PHONE = "000000000"
        const val INCOGNITO_PRIVATE_KEY = "0x123000000000"
        const val INCOGNITO_PUBLIC_KEY = "0x456000000000"
        const val INCOGNITO_EMAIL = "incognito@email.com"
        const val NEW_IDENTITY = "New Identity"
        const val NEW_IDENTITY_PUBLIC_KEY = "0x0000000000"
        const val NEW_IDENTITY_LOGO_LETTER = "+"
    }
}