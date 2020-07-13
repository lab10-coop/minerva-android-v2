package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ARTIS_TAU
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_CLASSIC_KOTTI
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.RSK_TRSK


@Retention(AnnotationRetention.SOURCE)
@StringDef(ARTIS_TAU, ETH_ROP, RSK_TRSK, ETH_RIN, ETH_KOV, ETH_GOR, ETH_CLASSIC_KOTTI, POA_SKL, LUKSO_14)
annotation class NetworkFullName {
    companion object {
        const val ARTIS_TAU = "ARTIS (Tau1)"
        const val ETH_ROP = "Ethereum (Ropsten)"
        const val RSK_TRSK = "RSK (Testnet)"
        const val ETH_RIN = "Ethereum (Rinkeby)"
        const val ETH_KOV = "Ethereum (Kovan)"
        const val ETH_GOR = "Ethereum (Görli)"
        const val ETH_CLASSIC_KOTTI = "Ethereum Classic (Kotti)"
        const val POA_SKL = "POA Network (Sokol)"
        const val LUKSO_14 = "LUKSO (L14)"
    }
}