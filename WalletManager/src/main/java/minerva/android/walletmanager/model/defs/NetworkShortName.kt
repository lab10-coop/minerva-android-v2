package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL

@Deprecated("Short value is deprecated. Use chainId instead")
@Retention(AnnotationRetention.SOURCE)
@StringDef(ATS_TAU, ETH_ROP, ETH_RIN, ETH_KOV, ETH_GOR, POA_SKL, LUKSO_14, ETH_MAIN)
annotation class NetworkShortName {
    companion object {
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ATS_TAU = "artis_tau1"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ETH_ROP = "eth_ropsten"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ETH_RIN = "eth_rinkeby"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ETH_KOV = "eth_kovan"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ETH_GOR = "eth_goerli"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val POA_SKL = "poa_sokol"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val LUKSO_14 = "lukso_l14"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ATS_SIGMA = "artis_sigma1"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val XDAI = "xdai"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val POA_CORE = "poa_core"
        @Deprecated("Short value is deprecated. Use chainId instead")
        const val ETH_MAIN = "eth_mainnet"
    }
}