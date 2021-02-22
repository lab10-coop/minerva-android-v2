package minerva.android.walletmanager.manager.networks

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.map.value
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import java.math.BigInteger

object NetworkManager {
    lateinit var networks: List<Network>

    @Deprecated("Network short is deprecated. Use chainId instead", ReplaceWith("Map<chainId(Int), Network>"))
    private val networkMap: Map<String, Network>
        get() = networks.associateBy { it.short }

    @Deprecated("Network short is deprecated. Use chainId instead")
    val httpsUrlMap: Map<String, String> by lazy { networks.associate { it.short to it.httpRpc } }

    @Deprecated("Network short is deprecated. Use chainId instead")
    val wssUrlMap: Map<String, String> by lazy { networks.associate { it.short to it.wsRpc } }

    @Deprecated("Network short is deprecated. Use chainId instead")
    val gasPriceMap: Map<String, BigInteger> by lazy { networks.associate { it.short to it.gasPrice } }

    fun initialize(networks: List<Network>) {
        if (areThereActiveNetworks(networks)) throw NoActiveNetworkThrowable()
        this.networks = networks.filter { isActiveNetwork(it) } + networks.filter { !isActiveNetwork(it) }
    }

    @Deprecated("Network short is deprecated. Use chainId instead")
    fun getNetwork(type: String): Network = networkMap.value(type)

    @Deprecated("Network short is deprecated. Use chainId instead")
    fun getChainId(type: String): Int = networkMap.value(type).chainId

    @Deprecated("Network short is deprecated. Use chainId instead")
    fun getShort(chainId: Int): String {
        networks.forEach {
            if (it.chainId == chainId) return it.short
        }
        throw NetworkNotFoundThrowable()
    }

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

    @Deprecated("Network short is deprecated. Use chainId instead")
    fun getTokens(network: String) = networkMap[network]?.tokens ?: listOf()

    fun isUsingEtherScan(network: String): Boolean =
        when (network) {
            ETH_MAIN, ETH_ROP, ETH_RIN, ETH_KOV, ETH_GOR -> true
            else -> false
        }

    private fun isActiveNetwork(network: Network): Boolean = network.httpRpc != String.Empty

    private fun areThereActiveNetworks(list: List<Network>): Boolean = list.none { isActiveNetwork(it) }
}

private const val ONE_ELEMENT = 1
private const val OPACITY_PREFIX = "#29"
private const val COLOR_LENGTH = 6