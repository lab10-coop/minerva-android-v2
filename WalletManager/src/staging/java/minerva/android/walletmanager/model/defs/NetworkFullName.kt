package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ATS
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH_CLASSIC
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.ETH
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.LUKSO
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.POA
import minerva.android.walletmanager.model.defs.NetworkFullName.Companion.RSK

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS, ETH, POA, LUKSO, RSK, ETH_CLASSIC)
annotation class NetworkFullName {
    companion object {
        const val ATS = "ARTIS (Tau1)"
        const val ETH = "Ethereum (Rinkeby)"
        const val POA = "POA (Sokol)"
        const val LUKSO = "LUKSO (L14)"
        const val RSK = "RSK (testnet)"
        const val ETH_CLASSIC = "Ethereum Classic (Kotti)"
    }
}