package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(CredentialName.OAMTC_AUTOMOTIVE_CARD)
annotation class CredentialName {
    companion object{
        const val OAMTC_AUTOMOTIVE_CARD = "ÖAMTC-Member Card"
    }
}