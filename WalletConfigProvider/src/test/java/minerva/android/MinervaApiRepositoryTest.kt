package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.*
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.logger.Logger
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigVersion
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.configProvider.repository.HttpNotFoundException
import minerva.android.configProvider.repository.MinervaApiRepositoryImpl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection

class MinervaApiRepositoryTest {

    private val api = mockk<MinervaApi>()
    private val logger = mockk<Logger>()
    private val repository = MinervaApiRepositoryImpl(api, logger)

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
    fun `get wallet config success`() {
        every { api.getWalletConfig(any(), any()) } returns Single.just("{version:1}")
        repository.getWalletConfig("publicKeys")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.version == 1
            }
    }

    @Test
    fun `get wallet config error`() {
        val error = Throwable()
        every { api.getWalletConfig(any(), any()) } returns Single.error(error)
        repository.getWalletConfig("publicKeys")
            .test()
            .assertError(error)
    }

    @Test
    fun `get wallet config http not found error test`() {
        val error = HttpException(
            Response.error<Any>(
                HttpURLConnection.HTTP_NOT_FOUND,
                "Test Server Error".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        )
        every { api.getWalletConfig(any(), any()) } returns Single.error(error)
        repository.getWalletConfig("publicKeys")
            .test()
            .assertError {
                it is HttpNotFoundException
            }
    }

    @Test
    fun `get wallet config version success`() {
        every { api.getWalletConfigVersion(any(), any()) } returns Single.just(WalletConfigVersion(2))
        repository.getWalletConfigVersion("publicKeys")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it == 2
            }
    }

    @Test
    fun `get wallet config version error`() {
        val error = Throwable()
        every { api.getWalletConfigVersion(any(), any()) } returns Single.error(error)
        repository.getWalletConfigVersion("publicKeys")
            .test()
            .assertError(error)
    }

    @Test
    fun `get wallet actions config success`() {
        every {
            api.getWalletActions(
                any(),
                any()
            )
        } returns Observable.just(WalletActionsResponse(_walletActionsConfigPayload = WalletActionsConfigPayload(_version = 1)))
        repository.getWalletActions("publicKeys")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.walletActionsConfigPayload.version == 1
            }
    }

    @Test
    fun `get wallet actions config error`() {
        val error = Throwable()
        every { api.getWalletActions(any(), any()) } returns Observable.error(error)
        repository.getWalletActions("publicKeys")
            .test()
            .assertError(error)
    }

    @Test
    fun `save wallet actions success`() {
        every { api.saveWalletActions(any(), any(), any()) } returns Completable.complete()
        repository.saveWalletActions("key", WalletActionsConfigPayload())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save wallet actions error`() {
        val error = Throwable()
        every { api.saveWalletActions(any(), any(), any()) } returns Completable.error(error)
        repository.saveWalletActions("key", WalletActionsConfigPayload())
            .test()
            .assertError(error)
    }

    @Test
    fun `save wallet config success`() {
        every { api.saveWalletConfig(any(), any(), any()) } returns Completable.complete()
        repository.saveWalletConfig("key", WalletConfigPayload(_version = 0))
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save wallet config error`() {
        val error = Throwable()
        every { api.saveWalletConfig(any(), any(), any()) } returns Completable.error(error)
        every { logger.logVersion(any(), any()) } returns Unit
        repository.saveWalletConfig("key", WalletConfigPayload(_version = 0))
            .test()
            .assertError(error)
    }

    @Test
    fun `save wallet config handle bad request error`() {
        val error = HttpException(
            Response.error<Any>(
                HttpURLConnection.HTTP_BAD_REQUEST,
                "Test Server Error".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        )
        every { api.saveWalletConfig(any(), any(), any()) } returns Completable.error(error)
        every { logger.logVersion(any(), any()) } returns Unit
        repository.saveWalletConfig("key", WalletConfigPayload(_version = 0))
            .test()
            .assertError {
                it is HttpBadRequestException
            }
    }
}