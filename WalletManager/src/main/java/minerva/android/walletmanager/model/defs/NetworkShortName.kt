package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_CLASSIC
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.RSK

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS, ETH, POA, LUKSO, RSK, ETH_CLASSIC)
annotation class NetworkShortName {
    companion object {
        const val ATS = "ATS"
        const val ETH = "ETH"
        const val POA = "POA"
        const val LUKSO = "LUKSO"
        const val RSK = "RSK"
        const val ETH_CLASSIC = "ETC"
    }
}