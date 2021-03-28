package minerva.android.widget.state

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AppUIStateTest {

    @Test
    fun `Check getting and updating Account UI State` () {
        val appUIState = AppUIStateImpl()
        appUIState.updateAccountState(1, true)
        appUIState.updateAccountState(3, false)
        val result01 = appUIState.getAccountUIState(1)
        val result02 = appUIState.getAccountUIState(3)
        val result03 = appUIState.getAccountUIState(13)
        result01 shouldBeEqualTo true
        result02 shouldBeEqualTo false
        result03 shouldBeEqualTo false
    }
}