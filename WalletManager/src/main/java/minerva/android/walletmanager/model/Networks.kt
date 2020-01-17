package minerva.android.walletmanager.model

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.Networks.Companion.ATS
import minerva.android.walletmanager.model.Networks.Companion.ETH
import minerva.android.walletmanager.model.Networks.Companion.POA
import minerva.android.walletmanager.model.Networks.Companion.XDAI

@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS, ETH, POA, XDAI)
annotation class Networks {
    companion object {
        const val ATS = "ATS"
        const val ETH = "ETH"
        const val POA = "POA"
        const val XDAI = "XDAI"
    }
}