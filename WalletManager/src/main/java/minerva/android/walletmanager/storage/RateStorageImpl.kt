package minerva.android.walletmanager.storage

import minerva.android.kotlinUtils.InvalidValue

class RateStorageImpl : RateStorage {

    private val rateMap = mutableMapOf<String, Rate>()

    override var areRatesSynced = false

    override fun getRates(): Map<String, Double> = rateMap.mapValues { it.value.value }

    override fun getRate(tokenHash: String) = rateMap[tokenHash]?.value ?: Double.InvalidValue

    override fun saveRate(tokenHash: String, rate: Double) {
        rateMap[tokenHash] = Rate(rate, System.currentTimeMillis())
    }

    override fun getLastUpdated(tokenHash: String) = rateMap[tokenHash]?.lastUpdated ?: Long.InvalidValue

    override fun shouldUpdateRate(tokenHash: String): Boolean =
        rateMap[tokenHash]?.let { rate ->
            (System.currentTimeMillis() - rate.lastUpdated) > SHOULD_UPDATE_THRESHOLD
        } ?: true

    override fun clearRates() {
        rateMap.clear()
    }

    private data class Rate(
        val value: Double = Double.InvalidValue,
        val lastUpdated: Long = Long.InvalidValue
    )

    companion object {
        const val TEN_MINUTES_AS_MILLISECONDS = 600000
        const val SHOULD_UPDATE_THRESHOLD = TEN_MINUTES_AS_MILLISECONDS
    }
}