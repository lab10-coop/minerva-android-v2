package minerva.android.walletmanager.repository.dapps

import io.reactivex.Single
import minerva.android.walletmanager.model.dapps.DappUIDetails

interface DappsRepository {
    fun getAllDappsDetails(): Single<List<DappUIDetails>>
    fun getDappForChainId(chainId: Int): Single<DappUIDetails>
}