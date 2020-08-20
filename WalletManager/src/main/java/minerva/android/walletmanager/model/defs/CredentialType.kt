package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(CredentialType.OAMTC)
annotation class CredentialType {
    //todo should be deleted when logo is downloaded from IPFS
    companion object {
        const val OAMTC = "did:ethr:artis_t1:0x01016a194e4d5beee3a634edb156f84d03354a03"
    }
}