package minerva.android.walletmanager.provider

import io.reactivex.Single

interface UnsupportedNetworkRepository {
    fun getNetworkName(chainId: Int): Single<String>
}