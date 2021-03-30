package minerva.android.widget.state

class AppUIStateImpl : AppUIState {
    private var accountStates = mutableMapOf<Int, Boolean>()

    override fun getAccountWidgetState(index: Int): Boolean = accountStates[index] ?: false

    override fun updateAccountWidgetState(index: Int, state: Boolean) {
        accountStates[index] = state
    }
}