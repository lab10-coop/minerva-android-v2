package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.model.defs.NetworkFullName
import minerva.android.walletmanager.model.defs.NetworkShortName
import java.math.BigInteger

enum class Network(val full: String, val short: String, val url: String, val gasPrice: BigInteger) {
    ARTIS(NetworkFullName.ATS, NetworkShortName.ATS, BuildConfig.ATS_ADDRESS, ARTIS_GAS_PRICE),
    ETHEREUM(NetworkFullName.ETH, NetworkShortName.ETH, BuildConfig.ETH_ADDRESS, ETHEREUM_GAS_PRICE),
    POA(NetworkFullName.POA, NetworkShortName.POA, BuildConfig.POA_ADDRESS, DEFAULT_GAS_PRICE),
    // Not available yet
    LUKSO(NetworkFullName.LUKSO, NetworkShortName.LUKSO, String.Empty, DEFAULT_GAS_PRICE),
    RSK(NetworkFullName.RSK, NetworkShortName.RSK, String.Empty, DEFAULT_GAS_PRICE),
    ETHEREUM_CLASSIC(NetworkFullName.ETH_CLASSIC, NetworkShortName.ETH_CLASSIC, String.Empty, DEFAULT_GAS_PRICE);

    companion object {
        private val map = values().associateBy(Network::short)
        val urlMap = values().associateBy(Network::short).mapValues { it.value.url }
        val gasPriceMap = values().associateBy(Network::short).mapValues { it.value.gasPrice }
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}

private val DEFAULT_GAS_PRICE: BigInteger = BigInteger.valueOf(1_000_000_000)
private val ETHEREUM_GAS_PRICE: BigInteger = BigInteger.valueOf(20_000_000_000)
private val ARTIS_GAS_PRICE: BigInteger = BigInteger.valueOf(100_000_000_000)