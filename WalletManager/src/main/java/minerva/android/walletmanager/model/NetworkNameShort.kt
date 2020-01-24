package minerva.android.walletmanager.model

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.NetworkNameShort.Companion.ATS
import minerva.android.walletmanager.model.NetworkNameShort.Companion.ETH
import minerva.android.walletmanager.model.NetworkNameShort.Companion.POA
import minerva.android.walletmanager.model.NetworkNameShort.Companion.XDAI

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