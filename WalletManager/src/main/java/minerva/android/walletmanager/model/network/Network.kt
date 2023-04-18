package minerva.android.walletmanager.model.network

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.BuildConfig
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
            1, 100, 137, 10, 324, 42161, 43114, 56, 42220, 30 -> String.format(BuildConfig.RPC_HTTP_URL, shortName())
            246529 -> "https://rpc.sigma1.artis.network"
            99 -> "https://core.poanetwork.dev"

            // testnets
            11155111 -> "https://rpc.sepolia.org/"
            5, 42, 4, 3 -> String.format(INFURA_HTTP_RPC_URL, shortName(), BuildConfig.INFURA_API_KEY)
            10200 -> "https://rpc.chiadochain.net"
            80001 -> "https://matic-mumbai.chainstacklabs.com"
            69 -> "https://kovan.optimism.io"
            420 -> "https://goerli.optimism.io"
            28528 -> "https://alpha-1-replica-2.bedrock-goerli.optimism.io/"
            280 -> "https://zksync2-testnet.zksync.dev"
            421613 -> "https://goerli-rollup.arbitrum.io/rpc"
            43113 -> "https://api.avax-test.network/ext/bc/C/rpc"
            97 -> "https://data-seed-prebsc-1-s1.binance.org:8545/"
            44787 -> "https://alfajores-forno.celo-testnet.org"
            62320 -> "https://baklava-forno.celo-testnet.org"
            2828 -> "https://rpc.l16.lukso.network"
            246785 -> "https://rpc.tau1.artis.network"
            421611 -> "https://rinkeby.arbitrum.io/rpc"
            22 -> "https://rpc.l14.lukso.network"
            77 -> "https://sokol.poa.network"
            31 -> "https://public-node.testnet.rsk.co"

            else -> ""
        }
    val gasPriceOracle: String = httpRpc
    val wsRpc: String get() =
        when (chainId) {
            // mainnets
            1, 100, 137, 10, 324, 42161, 43114, 56, 42220 -> String.format(BuildConfig.RPC_WS_URL, shortName())
            246529 -> "wss://ws.sigma1.artis.network"

            // testnets
            5, 42, 4, 3 -> String.format(INFURA_WS_RPC_URL, shortName(), BuildConfig.INFURA_API_KEY)
            69 -> "wss://ws-kovan.optimism.io"
            246785 -> "wss://ws.tau1.artis.network"
            77 -> "ws://sokol.poa.network:8546"

            else -> ""
        }

    private fun shortName() =
        when (chainId) {
            // mainnets
            1 -> "eth"
            100 -> "xdai"
            137 -> "matic"
            10 -> "optimism"
            324 -> "zksync"
            42161 -> "arbitrum"
            43114 -> "avalanche"
            56 -> "bsc"
            42220 -> "celo"
            30 -> "rsk"

            // testnets
            5 -> "goerli"
            42 -> "kovan"
            4 -> "rinkeby"
            3 -> "ropsten"

            else -> throw Error("ShortName not defined for $chainId")
        }

    companion object {
        const val INFURA_HTTP_RPC_URL = "https://%s.infura.io/v3/%s"
        const val INFURA_WS_RPC_URL = "https://%s.infura.io/v3/%s"
    }
}