package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.ATS20
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.DAI
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.FAU
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.SAI
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.SATS
import minerva.android.walletmanager.model.defs.AssetNameShort.Companion.SSAI

@Retention(AnnotationRetention.SOURCE)
@StringDef(SSAI, SATS, SAI, DAI, ATS20, FAU)
annotation class AssetNameShort {
    companion object {
        //ARTIS
        //main
        const val SSAI = "SSAI"
        //test
        const val SATS = "S-ATS"
        //ETHEREUM
        //main
        const val SAI = "SAI"
        const val DAI = "DAI"
        const val ATS20 = "ATS20"
        //test
        const val FAU = "FAU"
    }
}