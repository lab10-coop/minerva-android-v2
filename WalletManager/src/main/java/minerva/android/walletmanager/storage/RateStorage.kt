package minerva.android.walletmanager.storage

interface RateStorage  {
    var areRatesSynced: Boolean
    fun clearRates()
    fun getRates() : Map<String, Double>
    fun getRate(tokenHash: String): Double
    fun saveRate(tokenHash: String, rate: Double)
    fun getLastUpdated(tokenHash: String): Long
    fun shouldUpdateRate(tokenHash: String): Boolean
}