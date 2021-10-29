package minerva.android.services.dapps

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import minerva.android.BaseViewModelTest
import minerva.android.services.dapps.model.Dapp
import org.junit.Before
import org.junit.Test

class DappsViewModelTest : BaseViewModelTest() {
    private lateinit var viewModel: DappsViewModel

    private val dappsObserver: Observer<List<Dapp>> = mock()
    private val dappsCaptor: KArgumentCaptor<List<Dapp>> = argumentCaptor()

    @Before
    fun setup() {
        viewModel = DappsViewModel()
    }

    @Test
    fun `contains dapps on init success`() {
        viewModel.dappsLiveData.observeForever(dappsObserver)
        dappsCaptor.run {
            verify(dappsObserver).onChanged(capture())
            assert(firstValue.isNotEmpty())
        }
    }
}