package minerva.android.widget.state

class AppUIStateImpl : AppUIState {
    private var accountStates = mutableMapOf<Int, AccountWidgetState>()

    override fun getAccountWidgetState(index: Int): AccountWidgetState = (accountStates[index] ?: AccountWidgetState())

    override fun updateAccountWidgetState(index: Int, state: AccountWidgetState) {
        accountStates[index] = state
    }

    override var shouldShowSplashScreen: Boolean = false
}