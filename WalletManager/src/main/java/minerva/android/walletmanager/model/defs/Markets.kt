package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.Markets.Companion.ETH_EUR
import minerva.android.walletmanager.model.defs.Markets.Companion.POA_ETH
import minerva.android.walletmanager.model.defs.Markets.Companion.POA_EUR

@Retention(AnnotationRetention.SOURCE)
@StringDef(ETH_EUR, POA_ETH, POA_EUR)
annotation class Markets {
    companion object {
        const val ETH_EUR = "ETHEUR"
        const val POA_ETH = "POAETH"
        const val POA_EUR = "POAEUR"
        const val ETH_DAI = "ETHDAI"
        const val DAI_EUR = "DAIEUR"
    }
}