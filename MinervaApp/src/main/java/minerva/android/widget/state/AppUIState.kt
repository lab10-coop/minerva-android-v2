package minerva.android.widget.state

interface AppUIState {
    fun getAccountWidgetState(index: Int): AccountWidgetState
    fun updateAccountWidgetState(index: Int, state: AccountWidgetState)
}