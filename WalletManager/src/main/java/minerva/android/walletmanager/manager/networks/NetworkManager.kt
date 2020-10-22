package minerva.android.walletmanager.manager.networks

import androidx.annotation.VisibleForTesting
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.map.value
import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_NETWORK
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_NETWORK
import java.math.BigDecimal
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

    fun mapToAssets(assetList: List<Pair<String, BigDecimal>>): List<AccountAsset> = assetList.map { getAssetFromPair(it) }

    fun firstDefaultValueNetwork(): Network = networks[FIRST_NETWORK]

    fun secondDefaultValueNetwork(): Network =
        if (networks.size > ONE_ELEMENT && isActiveNetwork(networks[SECOND_NETWORK])) networks[SECOND_NETWORK]
        else firstDefaultValueNetwork()

    fun getStringColor(network: Network, opacity: Boolean): String {
        network.color.let { color ->
            return if (opacity) OPACITY_PREFIX + color.substring(color.length - COLOR_LENGTH, color.length)
            else color
        }
    }

    private fun getAssetFromPair(raw: Pair<String, BigDecimal>): AccountAsset {
        val asset = getAllAsset().find { it.address == raw.first } ?: Asset(address = raw.first)
        return AccountAsset(asset, raw.second)
    }

    @VisibleForTesting
    fun getAllAsset(): List<Asset> = mutableListOf<Asset>().apply { networks.forEach { addAll(it.assets) } }

    private fun isActiveNetwork(network: Network): Boolean = network.httpRpc != String.Empty

    private fun areThereActiveNetworks(list: List<Network>): Boolean = list.none { isActiveNetwork(it) }
}

private const val ONE_ELEMENT = 1
private const val OPACITY_PREFIX = "#29"
private const val COLOR_LENGTH = 6