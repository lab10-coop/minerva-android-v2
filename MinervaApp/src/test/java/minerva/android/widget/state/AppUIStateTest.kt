package minerva.android.widget.state

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AppUIStateTest {

    @Test
    fun `Check getting and updating Account UI State`() {
        val appUIState = AppUIStateImpl()
        val openWidgetState = AccountWidgetState().apply { isWidgetOpen = true }
        val closedWidgetState = AccountWidgetState().apply { isWidgetOpen = false }
        appUIState.updateAccountWidgetState(1, openWidgetState)
        appUIState.updateAccountWidgetState(3, closedWidgetState)
        val result01 = appUIState.getAccountWidgetState(1)
        val result02 = appUIState.getAccountWidgetState(3)
        val result03 = appUIState.getAccountWidgetState(13)
        result01.isWidgetOpen shouldBeEqualTo true
        result02.isWidgetOpen shouldBeEqualTo false
        result03.isWidgetOpen shouldBeEqualTo false
    }
}