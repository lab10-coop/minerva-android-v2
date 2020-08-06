package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(CredentialType.OAMTC)
annotation class CredentialType {
    companion object {
        const val OAMTC = "did:ethr:01016a194e4d5beee3a634edb156f84d03354a03"
    }
}