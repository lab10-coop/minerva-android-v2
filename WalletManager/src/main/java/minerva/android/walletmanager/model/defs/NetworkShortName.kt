package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_CLASSIC_KOTTI
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.RSK_TRSK

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS_TAU, ETH_ROP, RSK_TRSK, ETH_RIN, ETH_KOV, ETH_GOR, ETH_CLASSIC_KOTTI, POA_SKL, LUKSO_14)
annotation class NetworkShortName {
    companion object {
        const val ATS_TAU = "tats1"
        const val ETH_ROP = "rop"
        const val RSK_TRSK = "trsk"
        const val ETH_RIN = "rin"
        const val ETH_KOV = "kov"
        const val ETH_GOR = "gor"
        const val ETH_CLASSIC_KOTTI = "kot"
        const val POA_SKL = "skl"
        const val LUKSO_14 = "luk14"
    }
}