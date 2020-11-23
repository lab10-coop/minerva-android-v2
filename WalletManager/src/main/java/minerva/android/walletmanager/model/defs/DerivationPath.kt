package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.DerivationPath.Companion.DID_PATH
import minerva.android.walletmanager.model.defs.DerivationPath.Companion.MAIN_NET_PATH
import minerva.android.walletmanager.model.defs.DerivationPath.Companion.MASTER_KEYS_PATH
import minerva.android.walletmanager.model.defs.DerivationPath.Companion.TEST_NET_PATH

@Retention(AnnotationRetention.SOURCE)
@StringDef(MASTER_KEYS_PATH, DID_PATH, MAIN_NET_PATH, TEST_NET_PATH)
annotation class DerivationPath {
    companion object {
        const val MASTER_KEYS_PATH = "m/"
        const val DID_PATH = "m/73'/0'/0'/"
        const val MAIN_NET_PATH = "m/44'/60'/0'/0/"
        const val TEST_NET_PATH = "m/44'/1'/0'/0/"
    }
}