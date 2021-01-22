package minerva.android.walletmanager.manager.networks

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.map.value
import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import java.math.BigInteger

object NetworkManager {
    lateinit var networks: List<Network>
    private val networkMap: Map<String, Network> get() = networks.associateBy { it.short }
    val httpsUrlMap: Map<String, String> by lazy { networks.associate { it.short to it.httpRpc } }
    val wssUrlMap: Map<String, String> by lazy { networks.associate { it.short to it.wsRpc } }
    val gasPriceMap: Map<String, BigInteger> by lazy { networks.associate { it.short to it.gasPrice } }

    fun initialize(networks: List<Network>) {
        if (areThereActiveNetworks(networks)) throw NoActiveNetworkThrowable()
        this.networks = networks.filter { isActiveNetwork(it) } + networks.filter { !isActiveNetwork(it) }
    }

    fun getNetwork(type: String): Network = networkMap.value(type)

    fun firstDefaultValueNetwork(): Network = networks[FIRST_DEFAULT_NETWORK_INDEX]

    fun getNetworkByIndex(index: Int): Network =
        if (networks.size > ONE_ELEMENT && isActiveNetwork(networks[index])) networks[index]
        else firstDefaultValueNetwork()

    fun getStringColor(network: Network, opacity: Boolean): String {
        network.color.let { color ->
            return if (opacity) OPACITY_PREFIX + color.substring(color.length - COLOR_LENGTH, color.length)
            else color
        }
    }

    fun getTokens(network: String) = networkMap[network]?.tokens ?: listOf()

    private fun isActiveNetwork(network: Network): Boolean = network.httpRpc != String.Empty

    private fun areThereActiveNetworks(list: List<Network>): Boolean = list.none { isActiveNetwork(it) }
}

private const val ONE_ELEMENT = 1
private const val OPACITY_PREFIX = "#29"
private const val COLOR_LENGTH = 6