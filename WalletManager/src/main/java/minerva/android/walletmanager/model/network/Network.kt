package minerva.android.walletmanager.model.network

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_ONE
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_GOR
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
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_16
import minerva.android.walletmanager.model.defs.ChainId.Companion.POLYGON
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_BED
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZKS_ALPHA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZKS_ERA
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_TEST
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO_CHAI

import minerva.android.walletmanager.model.token.ERCToken
import java.math.BigInteger

data class Network(
    val name: String = String.Empty,
    val isActive: Boolean = true,
    val token: String = String.Empty,
    val isSafeAccountAvailable: Boolean = false,
    val gasPrice: BigInteger = BigInteger.TEN,
    val minGasPrice: BigInteger = BigInteger.TEN,
    val tokens: List<ERCToken> = emptyList(),
    val color: String = String.Empty,
    val testNet: Boolean = true,
    val chainId: Int = Int.InvalidValue,
    //base part of web path to explore transaction(web page address for getting info about specified transaction)
    val explore: String = String.Empty,
    val superfluid: SuperFluid? = null
) {
    fun isAvailable(): Boolean = httpRpc != String.Empty

    val httpRpc: String get() =
        when (chainId) {
            // mainnets
            ETH_MAIN, GNO, POLYGON, OPT, ZKS_ERA, ARB_ONE, AVA_C, BSC, CELO, RSK_MAIN ->
                String.format(BuildConfig.RPC_HTTP_URL, shortName())
            ATS_SIGMA -> ATS_SIGMA_HTTP_RPC_URL
            POA_CORE -> POA_CORE_HTTP_RPC_URL

            // testnets
            ETH_SEP -> ETH_SEP_HTTP_RPC_URL
            ETH_GOR, ETH_KOV, ETH_RIN, ETH_ROP -> String.format(INFURA_HTTP_RPC_URL, shortName(), BuildConfig.INFURA_API_KEY)
            GNO_CHAI -> GNO_CHIA_HTTP_RPC_URL
            MUMBAI -> MUMBAI_HTTP_RPC_URL
            OPT_KOV -> OPT_KOV_HTTP_RPC_URL
            OPT_GOR -> OPT_GOR_HTTP_RPC_URL
            OPT_BED -> OPT_BED_HTTP_RPC_URL
            ZKS_ALPHA -> ZKS_ALPHA_HTTP_RPC_URL
            ARB_GOR -> ARB_GOR_HTTP_RPC_URL
            AVA_FUJ -> AVA_FUJ_HTTP_RPC_URL
            BSC_TESTNET -> BSC_TESTNET_HTTP_RPC_URL
            CELO_ALF -> CELO_ALF_HTTP_RPC_URL
            CELO_BAK -> CELO_BAK_HTTP_RPC_URL
            LUKSO_16 -> LUKSO_16_HTTP_RPC_URL
            ATS_TAU -> ATS_TAU_HTTP_RPC_URL
            ARB_RIN -> ARB_RIN_HTTP_RPC_URL
            LUKSO_14 -> LUKSO_14_HTTP_RPC_URL
            POA_SKL -> POA_SKL_HTTP_RPC_URL
            RSK_TEST -> RSK_MAIN_HTTP_RPC_URL

            else -> ""
        }
    val gasPriceOracle: String = httpRpc
    val wsRpc: String get() =
        when (chainId) {
            // mainnets
            ETH_MAIN, GNO, POLYGON, OPT, ZKS_ERA, 42161, 43114, 56, 42220 -> String.format(BuildConfig.RPC_WS_URL, shortName())
            ATS_SIGMA -> ATS_SIGMA_WS_RPC_URL

            // testnets
            ETH_GOR, ETH_KOV, ETH_RIN, ETH_ROP -> String.format(INFURA_WS_RPC_URL, shortName(), BuildConfig.INFURA_API_KEY)
            OPT_KOV -> ETH_KOV_WS_RPC_URL
            ATS_TAU -> ATS_TAU_WS_RPC_URL
            POA_SKL -> POA_SKL_WS_RPC_URL

            else -> ""
        }

    private fun shortName() =
        when (chainId) {
            // mainnets
            ETH_MAIN -> ETH_MAIN_SHORT_NAME
            GNO -> GNO_SHORT_NAME
            POLYGON -> POLYGON_SHORT_NAME
            OPT -> OPT_SHORT_NAME
            ZKS_ERA -> ZKS_ERA_SHORT_NAME
            ARB_ONE -> ARB_ONE_SHORT_NAME
            AVA_C -> AVA_C_SHORT_NAME
            BSC -> BSC_SHORT_NAME
            CELO -> CELO_SHORT_NAME
            RSK_MAIN -> RSK_MAIN_SHORT_NAME

            // testnets
            ETH_GOR -> ETH_GOR_SHORT_NAME
            ETH_KOV -> ETH_KOV_SIGMA_SHORT_NAME
            ETH_RIN -> ETH_RIN_SHORT_NAME
            ETH_ROP -> ETH_ROP_SHORT_NAME

            else -> throw Error("ShortName not defined for $chainId")
        }

    companion object {
        // mainnets
        const val ATS_SIGMA_HTTP_RPC_URL = "https://rpc.sigma1.artis.network"
        const val POA_CORE_HTTP_RPC_URL = "https://core.poanetwork.dev"

        // testnets
        const val ETH_SEP_HTTP_RPC_URL = "https://rpc.sepolia.org/"
        const val GNO_CHIA_HTTP_RPC_URL = "https://rpc.chiadochain.net"
        const val MUMBAI_HTTP_RPC_URL = "https://matic-mumbai.chainstacklabs.com"
        const val OPT_KOV_HTTP_RPC_URL = "https://kovan.optimism.io"
        const val OPT_GOR_HTTP_RPC_URL = "https://goerli.optimism.io"
        const val OPT_BED_HTTP_RPC_URL = "https://alpha-1-replica-2.bedrock-goerli.optimism.io/"
        const val ZKS_ALPHA_HTTP_RPC_URL = "https://zksync2-testnet.zksync.dev"
        const val ARB_GOR_HTTP_RPC_URL = "https://goerli-rollup.arbitrum.io/rpc"
        const val AVA_FUJ_HTTP_RPC_URL = "https://api.avax-test.network/ext/bc/C/rpc"
        const val BSC_TESTNET_HTTP_RPC_URL = "https://data-seed-prebsc-1-s1.binance.org:8545/"
        const val CELO_ALF_HTTP_RPC_URL = "https://alfajores-forno.celo-testnet.org"
        const val CELO_BAK_HTTP_RPC_URL = "https://baklava-forno.celo-testnet.org"
        const val LUKSO_16_HTTP_RPC_URL = "https://rpc.l16.lukso.network"
        const val ATS_TAU_HTTP_RPC_URL = "https://rpc.tau1.artis.network"
        const val ARB_RIN_HTTP_RPC_URL = "https://rinkeby.arbitrum.io/rpc"
        const val LUKSO_14_HTTP_RPC_URL = "https://rpc.l14.lukso.network"
        const val POA_SKL_HTTP_RPC_URL = "https://sokol.poa.network"
        const val RSK_MAIN_HTTP_RPC_URL = "https://public-node.testnet.rsk.co"

        // mainnets
        const val ATS_SIGMA_WS_RPC_URL = "wss://ws.sigma1.artis.network"

        // testnets
        const val ETH_KOV_WS_RPC_URL = "wss://ws-kovan.optimism.io"
        const val ATS_TAU_WS_RPC_URL = "wss://ws.tau1.artis.network"
        const val POA_SKL_WS_RPC_URL = "ws://sokol.poa.network:8546"


        // mainnets
        const val GNO_SHORT_NAME = "xdai"
        const val POLYGON_SHORT_NAME = "matic"
        const val BSC_SHORT_NAME = "bsc"
        const val ETH_MAIN_SHORT_NAME = "eth"
        const val ARB_ONE_SHORT_NAME = "arbitrum"
        const val OPT_SHORT_NAME = "optimism"
        const val CELO_SHORT_NAME = "celo"
        const val AVA_C_SHORT_NAME = "avalanche"
        const val ZKS_ERA_SHORT_NAME = "zksync"

        // testnets
        const val RSK_MAIN_SHORT_NAME = "rsk"
        const val ETH_GOR_SHORT_NAME = "goerli"
        const val ETH_KOV_SIGMA_SHORT_NAME = "kovan"
        const val ETH_RIN_SHORT_NAME = "rinkeby"
        const val ETH_ROP_SHORT_NAME = "ropsten"
        const val ATS_SIGMA_SHORT_NAME = "artis_s1"

        const val INFURA_HTTP_RPC_URL = "https://%s.infura.io/v3/%s"
        const val INFURA_WS_RPC_URL = "https://%s.infura.io/v3/%s"
    }
}