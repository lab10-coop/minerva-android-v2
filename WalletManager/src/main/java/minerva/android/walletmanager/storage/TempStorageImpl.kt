package minerva.android.walletmanager.storage

import minerva.android.kotlinUtils.InvalidValue

class TempStorageImpl : TempStorage {

    private val rateMap = mutableMapOf<String, Double>()

    override fun clearRates() {
        rateMap.clear()
    }

    override fun getRates(): Map<String, Double> = rateMap

    override fun getRate(tokenHash: String) = rateMap[tokenHash] ?: Double.InvalidValue

    override fun saveRate(tokenHash: String, rate: Double) {
        rateMap[tokenHash] = rate
    }

}