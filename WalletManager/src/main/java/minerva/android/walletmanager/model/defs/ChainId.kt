package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_ONE
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_C
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_FUJ
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC_TESTNET
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO_ALF
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO_BAK
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_SEP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_TEST
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO_CHAI

// why isn't this taken from networks.json?
@Retention(AnnotationRetention.SOURCE)
@IntDef(ATS_TAU, ETH_ROP, ETH_RIN, ETH_KOV, ETH_GOR, ETH_SEP, POA_SKL, LUKSO_14, ETH_MAIN, ATS_SIGMA, GNO, GNO_CHAI, POA_CORE, MATIC, MUMBAI, BSC, BSC_TESTNET, RSK_MAIN, RSK_TEST, ARB_ONE, ARB_RIN, OPT, OPT_KOV, OPT_GOR, CELO, CELO_ALF, CELO_BAK, AVA_C, AVA_FUJ)
annotation class ChainId {
    companion object {
        const val ATS_TAU = 246785
        const val ETH_ROP = 3
        const val ETH_RIN = 4
        const val ETH_KOV = 42
        const val ETH_GOR = 5
        const val ETH_SEP = 11155111
        const val POA_SKL = 77
        const val LUKSO_14 = 22
        const val ATS_SIGMA = 246529
        const val GNO = 100
        const val GNO_CHAI = 10200
        const val POA_CORE = 99
        const val ETH_MAIN = 1
        const val MATIC = 137
        const val MUMBAI = 80001
        const val BSC = 56
        const val BSC_TESTNET = 97
        const val RSK_MAIN = 30
        const val RSK_TEST = 31
        const val ARB_ONE = 42161
        const val ARB_RIN = 421611
        const val OPT = 10
        const val OPT_KOV = 69
        const val OPT_GOR = 420
        const val CELO = 42220
        const val CELO_ALF = 44787
        const val CELO_BAK = 62320
        const val AVA_C = 43114
        const val AVA_FUJ = 43113
    }
}
