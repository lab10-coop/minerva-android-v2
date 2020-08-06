package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(VerifiableCredentialType.AUTOMOTIVE_CLUB)
annotation class VerifiableCredentialType {
    companion object {
        const val AUTOMOTIVE_CLUB = "AutomotiveMembershipCardCredential"
    }
}