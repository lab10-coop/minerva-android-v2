package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ARTIS_TAU
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ETH_CLASSIC_KOTTI
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkURL.Companion.RSK_TRSK

@Retention(AnnotationRetention.SOURCE)
@StringDef(ARTIS_TAU, ETH_ROP, RSK_TRSK, ETH_RIN, ETH_KOV, ETH_GOR, ETH_CLASSIC_KOTTI, POA_SKL, LUKSO_14)
annotation class NetworkURL {
    companion object {
        const val ARTIS_TAU = "https://rpc.tau1.artis.network"
        const val ETH_ROP = ""
        const val RSK_TRSK = ""
        const val ETH_RIN = "https://rinkeby.infura.io/v3/c7ec643b8c764cb5930bca18fb763469"
        const val ETH_KOV = ""
        const val ETH_GOR = ""
        const val ETH_CLASSIC_KOTTI = ""
        const val POA_SKL = "https://sokol.poa.network"
        const val LUKSO_14 = ""
    }
    }