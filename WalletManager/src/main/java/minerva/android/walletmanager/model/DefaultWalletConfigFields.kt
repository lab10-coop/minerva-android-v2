package minerva.android.walletmanager.model

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ARTIS_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ETHEREUM_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME

@Retention(AnnotationRetention.SOURCE)
@StringDef(DEFAULT_IDENTITY_NAME, DEFAULT_ARTIS_NAME, DEFAULT_ETHEREUM_NAME)
annotation class DefaultWalletConfigFields {
    companion object {
        const val DEFAULT_IDENTITY_NAME = "Identity #1"
        const val DEFAULT_ARTIS_NAME = "#1 ARTIS"
        const val DEFAULT_ETHEREUM_NAME = "#2 Ethereum"
        const val INCOGNITO_IDENTITY = "Incognito Identity"
        const val INCOGNITO_NAME = "Incognito mode"
        const val INCOGNITO_PHONE = "000000000"
    }
}