package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI

@Retention(AnnotationRetention.SOURCE)
@IntDef(ATS_TAU, ETH_ROP, ETH_RIN, ETH_KOV, ETH_GOR, POA_SKL, LUKSO_14, ETH_MAIN, ATS_SIGMA, XDAI, POA_CORE)
annotation class ChainId {
    companion object {
        const val ATS_TAU = 246785
        const val ETH_ROP = 3
        const val ETH_RIN = 4
        const val ETH_KOV = 42
        const val ETH_GOR = 5
        const val POA_SKL = 77
        const val LUKSO_14 = 22
        const val ATS_SIGMA = 246529
        const val XDAI = 100
        const val POA_CORE = 99
        const val ETH_MAIN = 1
    }
}
