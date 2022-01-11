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
    fun `get dapps with sponsored success`() {
        val dappsUIDetails = listOf(
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", false, 0
            ),
            DappUIDetails(
                "short", "subtitle", "long", "connectLink",
                "buttonColor", "iconLink", true, 1
            )
        )

        val dapps = DappsWithCategories(
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", true, 1
                )
            ),
            listOf(
                Dapp(
                    "short", "long", "subtitle", "buttonColor",
                    "iconLink", "connectLink", false, 0
                )
            )
        )

        whenever(dappRepository.getAllDappsDetails()).thenReturn(Single.just(dappsUIDetails))
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(true, firstValue.isSponsoredVisible)
        }
    }

    @Test
    fun `get dapps without sponsored success`() {
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
        viewModel.getDapps()
        viewModel.dappsLiveData.observeForever(dappsObserver)

        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assertEquals(dapps, firstValue)
            assertEquals(false, firstValue.isSponsoredVisible)
        }
    }
}