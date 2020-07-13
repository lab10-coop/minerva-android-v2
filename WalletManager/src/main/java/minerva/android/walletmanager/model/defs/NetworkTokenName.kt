package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ETH_CLASSIC_KOTTI
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkTokenName.Companion.RSK_TRSK

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS_TAU, ETH_ROP, RSK_TRSK, ETH_RIN, ETH_KOV, ETH_GOR, ETH_CLASSIC_KOTTI, POA_SKL, LUKSO_14)
annotation class NetworkTokenName {
    companion object {
        const val ATS_TAU = "ATS"
        const val ETH_ROP = "ETH"
        const val RSK_TRSK = "RSK"
        const val ETH_RIN = "ETH"
        const val ETH_KOV = "ETH"
        const val ETH_GOR = "ETH"
        const val ETH_CLASSIC_KOTTI = "ETC"
        const val POA_SKL = "POA"
        const val LUKSO_14 = "LUKSO"
    }
}