package minerva.android.walletmanager.provider

import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.kotlinUtils.Empty

class UnsupportedNetworkRepositoryImpl(
    private val cryptoApi: CryptoApi
) : UnsupportedNetworkRepository {
    override fun getNetworkName(chainId: Int): Single<String> {
        return cryptoApi.getChainDetails().map { data ->
            data.find { chainDetails ->
                chainId.toString() == chainDetails.chainId
            }?.name ?: String.Empty
        }
    }
}