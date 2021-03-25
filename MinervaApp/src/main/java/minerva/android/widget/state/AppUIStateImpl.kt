package minerva.android.widget.state

class AppUIStateImpl : AppUIState {
    private var accountStates = mutableMapOf<Int, Boolean>()

    override fun getAccountState(index: Int): Boolean = accountStates[index] ?: false

    override fun updateAccountState(index: Int, state: Boolean) {
        accountStates[index] = state
    }
}