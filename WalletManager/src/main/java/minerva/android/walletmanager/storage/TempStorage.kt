package minerva.android.walletmanager.storage

interface TempStorage  {
    fun clearRates()
    fun getRates() : Map<String, Double>
    fun getRate(tokenHash: String): Double
    fun saveRate(tokenHash: String, rate: Double)
}