package minerva.android.walletmanager.model.defs

fun getTransactionType(chainId: Int): TxType =
    when (chainId) {
        ChainId.ATS_TAU -> TxType.ARTIS_TAU
        ChainId.ATS_SIGMA -> TxType.ATS_SIGMA
        ChainId.ETH_RIN -> TxType.ETH_RIN
        ChainId.ETH_KOV -> TxType.ETH_KOVAN
        ChainId.POA_SKL -> TxType.SOKOL
        ChainId.ETH_GOR -> TxType.GORLI
        ChainId.ETH_SEP -> TxType.ETH_SEP
        ChainId.ETH_ROP -> TxType.ETH_ROPSTEN
        ChainId.LUKSO_14, ChainId.LUKSO_16 -> TxType.LUKSO
        ChainId.GNO, ChainId.GNO_CHAI -> TxType.GNO
        ChainId.POA_CORE -> TxType.POA_CORE
        ChainId.ARB_ONE, ChainId.ARB_RIN, ChainId.ARB_GOR -> TxType.ARB
        ChainId.OPT, ChainId.OPT_KOV, ChainId.OPT_GOR, ChainId.OPT_BED -> TxType.OPT
        ChainId.ZKS_ERA, ChainId.ZKS_ALPHA -> TxType.ZKS
        ChainId.ZK_EVM -> TxType.ZK_EVM
        ChainId.CELO, ChainId.CELO_ALF, ChainId.CELO_BAK -> TxType.CELO
        ChainId.AVA_C, ChainId.AVA_FUJ -> TxType.AVA
        else -> TxType.STANDARD
    }

enum class TxType(val time: String) {
    RAPID("~15 sec"), FAST("~1 min"), STANDARD("~3 min"), SLOW(">10min"),
    ARTIS_TAU("~5 sec"), ETH_RIN("~15 sec"), ETH_KOVAN("~4 sec"), LUKSO("~5 sec"), SOKOL("~5 sec"),
    GORLI("~14 sec"), ETH_ROPSTEN("~15 sec"), GNO("~5.2 sec"), POA_CORE("~5 sec"), ATS_SIGMA("~5 sec"),
    ARB("~2 sec"), OPT("~5 sec"), ZKS("~2 sec"), ZK_EVM("~2 sec"), CELO("~5 sec"), AVA("~2 sec"), ETH_SEP("~5 sec")
}