package minerva.android.walletmanager.model

import minerva.android.walletmanager.model.defs.NetworkFullName
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.NetworkTokenName
import minerva.android.walletmanager.model.defs.NetworkURL
import java.math.BigInteger

enum class Network(val full: String, val short: String, val token: String, val url: String, val gasPrice: BigInteger) {
    ATS_TAU(NetworkFullName.ARTIS_TAU, NetworkShortName.ATS_TAU, NetworkTokenName.ATS_TAU, NetworkURL.ARTIS_TAU, ARTIS_GAS_PRICE),
    ETH_RIN(NetworkFullName.ETH_RIN, NetworkShortName.ETH_RIN, NetworkTokenName.ETH_RIN, NetworkURL.ETH_RIN, ETHEREUM_GAS_PRICE),
    POA_SKL(NetworkFullName.POA_SKL, NetworkShortName.POA_SKL, NetworkTokenName.POA_SKL, NetworkURL.POA_SKL, DEFAULT_GAS_PRICE),

    // Not available yet
    ETH_ROP(NetworkFullName.ETH_ROP, NetworkShortName.ETH_ROP, NetworkTokenName.ETH_ROP, NetworkURL.ETH_ROP, ETHEREUM_GAS_PRICE),
    RSK_TRSK(NetworkFullName.RSK_TRSK, NetworkShortName.RSK_TRSK, NetworkTokenName.RSK_TRSK, NetworkURL.RSK_TRSK, DEFAULT_GAS_PRICE),
    ETH_KOV(NetworkFullName.ETH_KOV, NetworkShortName.ETH_KOV, NetworkTokenName.ETH_KOV, NetworkURL.ETH_KOV, ETHEREUM_GAS_PRICE),
    ETH_GOR(NetworkFullName.ETH_GOR, NetworkShortName.ETH_GOR, NetworkTokenName.ETH_GOR, NetworkURL.ETH_GOR, ETHEREUM_GAS_PRICE),
    ETH_CLASSIC_KOTTI(
        NetworkFullName.ETH_CLASSIC_KOTTI,
        NetworkShortName.ETH_CLASSIC_KOTTI,
        NetworkTokenName.ETH_CLASSIC_KOTTI,
        NetworkURL.ETH_CLASSIC_KOTTI,
        ETHEREUM_GAS_PRICE
    ),
    LUKSO_14(NetworkFullName.LUKSO_14, NetworkShortName.LUKSO_14, NetworkTokenName.LUKSO_14, NetworkURL.LUKSO_14, DEFAULT_GAS_PRICE);

    companion object {
        private val map = values().associateBy(Network::short)
        private val tokenMap = values().associateBy(Network::token)
        val urlMap = values().associateBy(Network::short).mapValues { it.value.url }
        val gasPriceMap = values().associateBy(Network::short).mapValues { it.value.gasPrice }
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
        fun hasSafeAccountOption(type: String) = type == ATS_TAU.short
    }
}


private val DEFAULT_GAS_PRICE: BigInteger = BigInteger.valueOf(1_000_000_000)
private val ETHEREUM_GAS_PRICE: BigInteger = BigInteger.valueOf(20_000_000_000)
private val ARTIS_GAS_PRICE: BigInteger = BigInteger.valueOf(100_000_000_000)