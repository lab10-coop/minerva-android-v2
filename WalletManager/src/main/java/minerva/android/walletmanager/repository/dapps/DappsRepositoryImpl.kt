package minerva.android.walletmanager.repository.dapps

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.DappDetails
import minerva.android.kotlinUtils.DateUtils
import minerva.android.walletmanager.database.dao.DappDao
import minerva.android.walletmanager.database.dao.FavoriteDappDao
import minerva.android.walletmanager.database.entity.DappEntity
import minerva.android.walletmanager.database.entity.FavoriteDappEntity
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage

class DappsRepositoryImpl(
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val dappDao: DappDao,
    private val favoriteDappDao: FavoriteDappDao
) : DappsRepository {

    private val currentTimeProvider = CurrentTimeProviderImpl()

    override fun insertFavoriteDapp(name: String): Completable =
        favoriteDappDao.insert(FavoriteDappEntity(name = name))


    override fun removeFavoriteDapp(name: String): Completable =
        favoriteDappDao.delete(name)

    override fun getFavoriteDapps(): Single<List<String>> =
        favoriteDappDao.getAllFavoriteDapps().map { list -> list.map { it.name } }

    override fun getDappForChainId(chainId: Int): Single<DappUIDetails> =
        dappDao.getSponsoredDappForChainId(chainId).map {
            it.mapToDappUIDetails()
        }

    override fun getAllDappsDetailsFromDB(): Single<List<DappUIDetails>> =
        dappDao.getAllDapps().map { dappEntityList -> dappEntityList.map { it.mapToDappUIDetails() } }

    override fun getAllDappsDetails(): Single<List<DappUIDetails>> =
        cryptoApi.getLastCommitFromDappsDetails().flatMap {
            getDappList(it)
        }.onErrorResumeNext {
            dappDao.getAllDapps()
        }.map { dappEntityList ->
            dappEntityList.map { it.mapToDappUIDetails() }
        }

    private fun getDappList(commits: List<CommitElement>): Single<List<DappEntity>> =
        if (isNewCommit(commits)) {
            fetchDappList()
        } else {
            dappDao.getAllDapps()
        }

    private fun isNewCommit(list: List<CommitElement>): Boolean =
        list[LAST_UPDATE_INDEX].lastCommitDate.let {
            localStorage.loadDappDetailsUpdateTimestamp() < DateUtils.getTimestampFromDate(it)
        }

    private fun fetchDappList(): Single<List<DappEntity>> =
        cryptoApi.getDappsDetails().map { list ->
            updateDetails(list.map { dappDetails -> dappDetails.mapToDappEntity() })
        }.onErrorResumeNext {
            dappDao.getAllDapps()
        }

    private fun updateDetails(list: List<DappEntity>): List<DappEntity> {
        dappDao.deleteAll()
        dappDao.insertAll(list)
        favoriteDappDao.deleteNotMatchingDapps(list.map { it.shortName })
        localStorage.saveDappDetailsUpdateTimestamp(currentTimeProvider.currentTimeMills())
        return list
    }

    private fun DappDetails.mapToDappEntity(): DappEntity =
        DappEntity(
            shortName = shortName,
            subtitle = subtitle,
            connectLink = connectLink,
            buttonColor = buttonColor,
            chainIds = chainIds,
            iconLink = iconLink,
            longName = longName,
            explainerTitle = explainerTitle,
            explainerText = explainerText,
            explainerSteps = explainerStepByStep,
            sponsored = sponsored,
            sponsoredChainId = sponsoredChainId
        )

    private fun DappEntity.mapToDappUIDetails(): DappUIDetails =
        DappUIDetails(
            shortName,
            subtitle,
            longName,
            connectLink,
            buttonColor,
            iconLink,
            sponsored != NOT_SPONSORED_ID,
            sponsored
        )

    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val NOT_SPONSORED_ID = 0
    }
}