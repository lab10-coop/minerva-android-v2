package minerva.android.walletmanager.manager.networks

import android.graphics.Color
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
    val urlMap: Map<String, String> by lazy { networks.associate { it.short to it.url } }
    val gasPriceMap: Map<String, BigInteger> by lazy { networks.associate { it.short to it.gasPrice } }

    fun initialize(networks: List<Network>) {
        if (noActiveNetworks(networks)) throw NoActiveNetworkThrowable()
        this.networks = networks.filter { isActiveNetwork(it) } + networks.filter { !isActiveNetwork(it) }
    }

    fun getNetwork(type: String) = networkMap.value(type)

    fun getAssetsAddresses(type: String): List<String> = getNetwork(type).assets.map { it.address }

    fun mapToAssets(assetList: List<Pair<String, BigDecimal>>): List<AccountAsset> = assetList.map {
        getAssetFromPair(
            it
        )
    }

    fun isSafeAccountAvailable(type: String) = getNetwork(type).isSafeAccountAvailable

    fun getColor(type: String, opacity: Boolean = false) = Color.parseColor(getStringColor(type, opacity))

    fun firstDefaultValueNetwork() = networks[FIRST_NETWORK]

    fun secondDefaultValueNetwork(): Network =
        if (networks.size > ONE_ELEMENT && isActiveNetwork(networks[SECOND_NETWORK])) networks[SECOND_NETWORK]
        else firstDefaultValueNetwork()

    fun isAvailable(type: String) = getNetwork(type).url != String.Empty

    @VisibleForTesting
    fun getStringColor(type: String, opacity: Boolean): String {
        getNetwork(type).color.let { color ->
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

    private fun isActiveNetwork(network: Network) = network.url != String.Empty

    private fun noActiveNetworks(list: List<Network>) = list.none { isActiveNetwork(it) }
}

private const val ONE_ELEMENT = 1
private const val OPACITY_PREFIX = "#29"
private const val COLOR_LENGTH = 6