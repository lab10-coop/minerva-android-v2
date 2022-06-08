package minerva.android.apiProvider.model

@Retention(AnnotationRetention.SOURCE)
annotation class MarketIds {
    companion object {
        // native coin: https://www.coingecko.com/api/documentations/v3#/coins/get_coins_list
        const val MATIC = "matic-network"
        const val BSC_COIN = "binancecoin"
        const val POA_NETWORK = "poa-network"
        const val AVAX = "avalanche-2"

        // asset_platform: https://www.coingecko.com/api/documentations/v3#/asset_platforms/get_asset_platforms
        const val POLYGON = "polygon-pos"
        const val BSC_TOKEN = "binance-smart-chain"
        const val ARB_ONE = "arbitrum-one"
        const val OPT = "optimistic-ethereum"
        const val AVA_C = "avalanche"

        // both: coin and asset_platform
        const val ETHEREUM = "ethereum"
        const val XDAI = "xdai"
        const val RSK = "rootstock"
        const val CELO = "celo"
    }
}
