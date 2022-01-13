package minerva.android.walletmanager.repository.dapps

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.Commit
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.Committer
import minerva.android.apiProvider.model.DappDetails
import minerva.android.walletmanager.database.dao.DappDao
import minerva.android.walletmanager.database.dao.FavoriteDappDao
import minerva.android.walletmanager.database.entity.DappEntity
import minerva.android.walletmanager.database.entity.FavoriteDappEntity
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.storage.LocalStorage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class DappsRepositoryImplTest {

    private val favoriteDappDao: FavoriteDappDao = mock()
    private val dappDao: DappDao = mock()
    private val cryptoApi: CryptoApi = mock()
    private val localStorage: LocalStorage = mock()

    private val repository: DappsRepository = DappsRepositoryImpl(cryptoApi, localStorage, dappDao, favoriteDappDao)

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills

    private val dappsDetails = listOf(
        DappDetails(
            "short", "subtitle", "connectLink",
            "buttonColor", emptyList(), "iconLink", "long", "explainerTitle", "explainerText", emptyList(),
            0, 1
        ),
        DappDetails(
            "short", "subtitle", "connectLink",
            "buttonColor", emptyList(), "iconLink", "long", "explainerTitle", "explainerText", emptyList(),
            1, 1
        )
    )

    private val dappsDetailsEntity = listOf(
        DappEntity(
            1, "short", "subtitle", "connectLink",
            "buttonColor", emptyList(), "iconLink", "long",
            "explainerTitle", "explainerText", emptyList(),
            0, 1
        ),
        DappEntity(
            2, "short", "subtitle", "connectLink",
            "buttonColor", emptyList(), "iconLink", "long",
            "explainerTitle", "explainerText", emptyList(),
            1, 1
        )
    )

    private val dappsUIDetails = listOf(
        DappUIDetails(
            "short", "subtitle", "long", "connectLink",
            "buttonColor", "iconLink", false, 0
        ),
        DappUIDetails(
            "short", "subtitle", "long", "connectLink",
            "buttonColor", "iconLink", true, 1
        )
    )

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `fetch dapp data from server test`() {
        whenever(cryptoApi.getLastCommitFromDappsDetails()).thenReturn(Single.just(commitData))
        whenever(cryptoApi.getDappsDetails()).thenReturn(Single.just(dappsDetails))
        whenever(localStorage.loadDappDetailsUpdateTimestamp()).thenReturn(1611950161999)
        whenever(favoriteDappDao.deleteNotMatchingDapps(any())).thenReturn(0)
        doNothing().whenever(localStorage).saveDappDetailsUpdateTimestamp(any())
        doNothing().whenever(dappDao).insertAll(any())
        doNothing().whenever(dappDao).deleteAll()

        repository.getAllDappsDetails().test()
            .await()
            .assertValue {
                it == dappsUIDetails
            }
    }

    @Test
    fun `get dapp data from db, when server returns error test`() {
        whenever(cryptoApi.getLastCommitFromDappsDetails()).thenReturn(Single.just(commitData))
        whenever(cryptoApi.getDappsDetails()).thenReturn(Single.error(Throwable()))
        whenever(dappDao.getAllDapps()).thenReturn(Single.just(dappsDetailsEntity))
        whenever(localStorage.loadDappDetailsUpdateTimestamp()).thenReturn(1611950161999)
        doNothing().whenever(localStorage).saveDappDetailsUpdateTimestamp(any())
        doNothing().whenever(dappDao).insertAll(any())
        doNothing().whenever(dappDao).deleteAll()

        repository.getAllDappsDetails().test()
            .await()
            .assertValue {
                it == dappsUIDetails
            }
    }

    @Test
    fun `get dapp data from db, when fetching commits returns error test`() {
        whenever(cryptoApi.getLastCommitFromDappsDetails()).thenReturn(Single.error(Throwable()))
        whenever(dappDao.getAllDapps()).thenReturn(Single.just(dappsDetailsEntity))

        repository.getAllDappsDetails().test()
            .await()
            .assertValue {
                it == dappsUIDetails
            }
    }

    @Test
    fun `get dapp data from db test`() {
        whenever(cryptoApi.getLastCommitFromDappsDetails()).thenReturn(Single.just(commitData))
        whenever(dappDao.getAllDapps()).thenReturn(Single.just(dappsDetailsEntity))
        whenever(localStorage.loadDappDetailsUpdateTimestamp()).thenReturn(1612950162222)

        repository.getAllDappsDetails().test()
            .await()
            .assertValue {
                it == dappsUIDetails
            }
    }

    @Test
    fun `get dapp data from db without checking server test`() {
        whenever(dappDao.getAllDapps()).thenReturn(Single.just(dappsDetailsEntity))
        repository.getAllDappsDetailsFromDB().test()
            .await()
            .assertValue {
                it == dappsUIDetails
            }
    }

    @Test
    fun `instert favorite dapp to db test`() {
        whenever(favoriteDappDao.insert(any())).thenReturn(Completable.complete())
        repository.insertFavoriteDapp("test").test()
            .await()
            .assertComplete()

        verify(favoriteDappDao).insert(FavoriteDappEntity(name = "test"))
    }

    @Test
    fun `remove favorite dapp to db test`() {
        whenever(favoriteDappDao.delete(any())).thenReturn(Completable.complete())
        repository.removeFavoriteDapp("test").test()
            .await()
            .assertComplete()

        verify(favoriteDappDao).delete("test")
    }

    @Test
    fun `get favorite dapps test`() {
        val favoriteDappEntities =
            listOf(FavoriteDappEntity(1, "test1"), FavoriteDappEntity(2, "test2"), FavoriteDappEntity(3, "test3"))
        val favoriteDappMap = listOf("test1", "test2", "test3")
        whenever(favoriteDappDao.getAllFavoriteDapps()).thenReturn(Single.just(favoriteDappEntities))
        repository.getFavoriteDapps().test()
            .await()
            .assertValue {
                it == favoriteDappMap
            }
    }


    @Test
    fun `get sponsored dapp by chain id from db test`() {
        val entity = DappEntity(
            2, "short", "subtitle", "connectLink",
            "buttonColor", emptyList(), "iconLink", "long",
            "explainerTitle", "explainerText", emptyList(),
            1, 1
        )
        val result = DappUIDetails(
            "short", "subtitle", "long", "connectLink",
            "buttonColor", "iconLink", true, 1
        )
        whenever(dappDao.getSponsoredDappForChainId(any())).thenReturn(Single.just(entity))

        repository.getDappForChainId(1).test()
            .await()
            .assertValue {
                it == result
            }
    }
}