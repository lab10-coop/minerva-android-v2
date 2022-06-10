package minerva.android.walletmanager.model.defs

import androidx.constraintlayout.solver.state.State


fun getTransactionType(chainId: Int): TxType =
    when (chainId) {
        ChainId.ATS_TAU -> TxType.ARTIS_TAU
        ChainId.ETH_RIN -> TxType.ETH_RIN
        ChainId.ETH_KOV -> TxType.ETH_KOVAN
        ChainId.LUKSO_14 -> TxType.LUKSO
        ChainId.POA_SKL -> TxType.SOKOL
        ChainId.ETH_GOR -> TxType.GORLI
        ChainId.ETH_SEP -> TxType.ETH_SEP
        ChainId.ATS_SIGMA -> TxType.ATS_SIGMA
        ChainId.XDAI -> TxType.XDAI
        ChainId.ETH_ROP -> TxType.ETH_ROPSTEN
        ChainId.POA_CORE -> TxType.POA_CORE
        ChainId.ARB_ONE -> TxType.ARB
        ChainId.ARB_RIN -> TxType.ARB
        ChainId.OPT -> TxType.OPT
        ChainId.OPT_KOV -> TxType.OPT
        ChainId.CELO -> TxType.CELO
        ChainId.CELO_ALF -> TxType.CELO
        ChainId.CELO_BAK -> TxType.CELO
        ChainId.AVA_C -> TxType.AVA
        ChainId.AVA_FUJ -> TxType.AVA
        else -> TxType.STANDARD
    }

enum class TxType(val time: String) {
    RAPID("~15 sec"), FAST("~1 min"), STANDARD("~3 min"), SLOW(">10min"),
    ARTIS_TAU("~5 sec"), ETH_RIN("~15 sec"), ETH_KOVAN("~4 sec"), LUKSO("~5 sec"), SOKOL("~5 sec"),
    GORLI("~14 sec"), ETH_ROPSTEN("~15 sec"), XDAI("~5.2 sec"), POA_CORE("~5 sec"), ATS_SIGMA("~5 sec"),
    ARB("~2 sec"), OPT("~5 sec"), CELO("~5 sec"), AVA("~2 sec"), ETH_SEP("~5 sec")
}