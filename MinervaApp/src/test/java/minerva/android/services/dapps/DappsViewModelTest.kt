package minerva.android.services.dapps

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.services.dapps.model.Dapp
import minerva.android.services.dapps.model.DappsWithCategories
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.repository.dapps.DappsRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DappsViewModelTest : BaseViewModelTest() {
    private lateinit var viewModel: DappsViewModel
    private val dappRepository: DappsRepository = mock()

    private val dappsObserver: Observer<DappsWithCategories> = mock()
    private val dappsCaptor: KArgumentCaptor<DappsWithCategories> = argumentCaptor()

    @Before
    fun setup() {
        viewModel = DappsViewModel(dappRepository)
    }

    @Test
    fun `get dapps with sponsored and favorites success`() {
        val dappsUIDetails = listOf(
            DappUIDetails(
                "bshortF1", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "ashortF2", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", true, 2
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", true, 1
            )
        )

        val dapps = DappsWithCategories(
            listOf(
                Dapp(
                    "ashortF2", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, true
                ),
                Dapp(
                    "bshortF1", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, true
                )
            ),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", true, 1, false
                ),
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", true, 2, false
                )
            ),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, false
                )
            )
        )

        whenever(dappRepository.getAllDappsDetails()).thenReturn(Single.just(dappsUIDetails))
        whenever(dappRepository.getFavoriteDapps()).thenReturn(Single.just(listOf("ashortF2", "bshortF1")))
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(true, firstValue.isSponsoredVisible)
            assertEquals(true, firstValue.isFavoriteVisible)
        }
    }

    @Test
    fun `get dapps without sponsored and with favorites success`() {
        val dappsUIDetails = listOf(
            DappUIDetails(
                "ashortF1", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "bshortF2", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            )
        )

        val dapps = DappsWithCategories(
            listOf(
                Dapp(
                    "ashortF1", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, true
                ),
                Dapp(
                    "bshortF2", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, true
                )
            ),
            emptyList(),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, false
                )
            )
        )

        whenever(dappRepository.getAllDappsDetails()).thenReturn(Single.just(dappsUIDetails))
        whenever(dappRepository.getFavoriteDapps()).thenReturn(Single.just(listOf("bshortF2", "ashortF1")))
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(false, firstValue.isSponsoredVisible)
            assertEquals(true, firstValue.isFavoriteVisible)
        }
    }

    @Test
    fun `get dapps with sponsored but without favorites success`() {
        val dappsUIDetails = listOf(
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", true, 2
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", true, 1
            )
        )

        val dapps = DappsWithCategories(
            emptyList(),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", true, 1, false
                ),
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", true, 2, false
                )
            ),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0, false
                )
            )
        )

        whenever(dappRepository.getAllDappsDetails()).thenReturn(Single.just(dappsUIDetails))
        whenever(dappRepository.getFavoriteDapps()).thenReturn(Single.just(emptyList()))
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(true, firstValue.isSponsoredVisible)
            assertEquals(false, firstValue.isFavoriteVisible)
        }
    }


    @Test
    fun `get dapps without sponsored and favorites success`() {
        val dappsUIDetails = listOf(
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            )
        )

        val dapps = DappsWithCategories(
            emptyList(),
            emptyList(),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0
                ),
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0
                )
            )
        )

        whenever(dappRepository.getAllDappsDetails()).thenReturn(Single.just(dappsUIDetails))
        whenever(dappRepository.getFavoriteDapps()).thenReturn(Single.just(emptyList()))
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(false, firstValue.isSponsoredVisible)
            assertEquals(false, firstValue.isFavoriteVisible)
        }
    }
}