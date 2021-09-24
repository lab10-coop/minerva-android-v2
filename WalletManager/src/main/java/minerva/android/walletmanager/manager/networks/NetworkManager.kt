package minerva.android.walletmanager.manager.networks

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.map.value
import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_TEST_NETWORK_INDEX
import minerva.android.walletmanager.model.network.Network
import java.math.BigInteger

object NetworkManager {
    lateinit var networks: List<Network>

    private val networkMap: Map<Int, Network>
        get() = networks.associateBy { it.chainId }

    val httpsUrlMap: Map<Int, String> by lazy { networks.associate { it.chainId to it.httpRpc } }

    val wssUrlMap: Map<Int, String> by lazy { networks.associate { it.chainId to it.wsRpc } }

    val gasPriceMap: Map<Int, BigInteger> by lazy { networks.associate { it.chainId to it.gasPrice } }

    fun initialize(networks: List<Network>) {
        if (areThereActiveNetworks(networks)) throw NoActiveNetworkThrowable()
        this.networks = networks.filter { isActiveNetwork(it) } + networks.filter { !isActiveNetwork(it) }
    }

    fun getNetwork(chainId: Int): Network = networkMap.value(chainId)

    fun firstDefaultValueNetwork(): Network = networks[FIRST_DEFAULT_TEST_NETWORK_INDEX]

    fun getNetworkByIndex(index: Int): Network =
        if (networks.size > ONE_ELEMENT && isActiveNetwork(networks[index])) networks[index]
        else firstDefaultValueNetwork()

    fun getStringColor(network: Network, opacity: Boolean): String {
        network.color.let { color ->
            return if (opacity) OPACITY_PREFIX + color.substring(color.length - COLOR_LENGTH, color.length)
            else color
        }
    }

    fun getTokens(chainId: Int) = networkMap[chainId]?.tokens ?: listOf()

    fun isUsingEtherScan(chainId: Int): Boolean =
        when (chainId) {
            ETH_MAIN, ETH_ROP, ETH_RIN, ETH_KOV, ETH_GOR -> true
            else -> false
        }

    private fun isActiveNetwork(network: Network): Boolean = network.httpRpc != String.Empty

    private fun areThereActiveNetworks(list: List<Network>): Boolean = list.none { isActiveNetwork(it) }
}

private const val ONE_ELEMENT = 1
private const val OPACITY_PREFIX = "#29"
private const val COLOR_LENGTH = 6