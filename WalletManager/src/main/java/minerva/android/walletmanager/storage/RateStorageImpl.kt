package minerva.android.walletmanager.storage

import minerva.android.kotlinUtils.InvalidValue

class RateStorageImpl : RateStorage {

    private val rateMap = mutableMapOf<String, Double>()

    override var areRatesSynced = false

    override fun getRates(): Map<String, Double> = rateMap

    override fun getRate(tokenHash: String) = rateMap[tokenHash] ?: Double.InvalidValue

    override fun saveRate(tokenHash: String, rate: Double) {
        rateMap[tokenHash] = rate
    }

    override fun clearRates() {
        rateMap.clear()
    }
}