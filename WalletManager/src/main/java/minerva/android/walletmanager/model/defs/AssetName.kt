package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.AssetName.Companion.ATS20
import minerva.android.walletmanager.model.defs.AssetName.Companion.DAI
import minerva.android.walletmanager.model.defs.AssetName.Companion.FAU
import minerva.android.walletmanager.model.defs.AssetName.Companion.SAI
import minerva.android.walletmanager.model.defs.AssetName.Companion.SFAU
import minerva.android.walletmanager.model.defs.AssetName.Companion.SSAI

@Retention(AnnotationRetention.SOURCE)
@StringDef(SSAI, SFAU, SAI, DAI, ATS20, FAU)
annotation class AssetName {
    //TODO get full names for all assets
    companion object {
        //ARTIS
        //main
        const val SSAI = "SSAI"
        //test
        const val SFAU = "FaucetToken"
        //ETHEREUM
        //main
        const val SAI = "SAI"
        const val DAI = "DAI"
        const val ATS20 = "ATS20"
        //test
        const val FAU = "FaucetToken"
    }
}