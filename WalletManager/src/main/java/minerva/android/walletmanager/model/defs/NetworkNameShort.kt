package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkNameShort.Companion.ATS
import minerva.android.walletmanager.model.defs.NetworkNameShort.Companion.ETH
import minerva.android.walletmanager.model.defs.NetworkNameShort.Companion.POA
import minerva.android.walletmanager.model.defs.NetworkNameShort.Companion.XDAI

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS, ETH, POA, XDAI)
annotation class NetworkNameShort {
    companion object {
        const val ATS = "ATS"
        const val ETH = "ETH"
        const val POA = "POA"
        const val XDAI = "XDAI"
    }
}