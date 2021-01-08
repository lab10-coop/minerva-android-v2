package minerva.android.apiProvider.model

@Retention(AnnotationRetention.SOURCE)
annotation class MarketIds {
    companion object {
        const val ETHEREUM = "ethereum"
        const val POA_NETWORK = "poa-network"
        const val DAI = "dai"
    }
}
