package minerva.android.apiProvider.model

@Retention(AnnotationRetention.SOURCE)
annotation class MarketIds {
    companion object {
        // native coin: https://www.coingecko.com/api/documentations/v3#/coins/get_coins_list
        const val MATIC = "matic-network"
        const val BSC_COIN = "binancecoin"
        const val POA_NETWORK = "poa-network"
        const val AVAX = "avalanche-2"
        const val ETHEREUM = "ethereum"
        const val GNO = "xdai"
        const val RSK = "rootstock"
        const val CELO = "celo"
    }
}
