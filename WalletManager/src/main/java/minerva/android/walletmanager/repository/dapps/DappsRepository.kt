package minerva.android.walletmanager.repository.dapps

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.dapps.DappUIDetails

interface DappsRepository {
    fun getAllDappsDetails(): Single<List<DappUIDetails>>
    fun getAllDappsDetailsFromDB(): Single<List<DappUIDetails>>
    fun getDappForChainId(chainId: Int): Single<DappUIDetails>
    fun insertFavoriteDapp(name: String): Completable
    fun removeFavoriteDapp(name: String): Completable
    fun getFavoriteDapps(): Single<List<String>>
}