package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.RSK
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_CLASSIC
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS, ETH, POA, LUKSO, RSK, ETH_CLASSIC)
annotation class NetworkFullName {
    companion object {
        const val ATS = "ARTIS"
        const val  ETH = "Ethereum"
        const val POA = "POA"
        const val LUKSO = "LUKSO"
        const val RSK = "RSK"
        const val ETH_CLASSIC = "Ethereum Classic"
    }
}