package minerva.android.widget.clubcard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.walletmanager.model.Credential
import minerva.android.widget.clubCard.CacheStorage
import minerva.android.widget.clubCard.ClubCardStateCallback
import minerva.android.widget.clubCard.ClubCardViewModel
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ClubCardViewModelTest {

    private val cacheStorage: CacheStorage = mock()
    private val callback: ClubCardStateCallback = mock(ClubCardStateCallback::class)

    private val viewModel = ClubCardViewModel(cacheStorage)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    open fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Error when cache data is null` () {
        whenever(cacheStorage.load(any())).thenReturn(null)
        viewModel.loadCardData(Credential(), callback)
        verify(callback, times(1)).onError()
    }

    @Test
    fun `Error when cache data is incorrect`() {
        whenever(cacheStorage.load(any())).thenReturn("")
        viewModel.loadCardData(Credential(), callback)
        verify(callback, times(1)).onError()
    }

    @Test
    fun `Correct read data from cache`() {
        whenever(cacheStorage.load(any())).thenReturn(pageSource)
        viewModel.loadCardData(Credential(), callback)
        verify(callback, times(1)).onCardDataPrepared(any())
    }


    private val pageSource = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 242 153\">\n" +
            "   <defs>\n" +
            "      <style>.textvalue{font-size:9px;font-family:ArialNarrow,Arial}</style>\n" +
            "   </defs>\n" +
            "   <clipPath id=\"card\">\n" +
            "      <rect class=\"card\" fill=\"#ffdc00\" width=\"242\" height=\"153\" rx=\"10\"/>\n" +
            "   </clipPath>\n" +
            "   <g id=\"Labels\">\n" +
            "      <text id=\"exp\" class=\"textvalue\" transform=\"translate(44.61 105.74)\">2025</text>\n" +
            "      <text id=\"name\" class=\"textvalue\" transform=\"translate(44.61 117.19)\">Maximilian Musterperson</text>\n" +
            "      <text id=\"since\" class=\"textvalue\" transform=\"translate(44.61 128.64)\">1970</text>\n" +
            "      <text id=\"memberId\" class=\"textvalue\" transform=\"translate(44.61 140.09)\">123456789</text>\n" +
            "      <text id=\"coverage\" class=\"textvalue\" transform=\"translate(100 140.09)\">Something</text>\n" +
            "   </g>\n" +
            "</svg>"

}