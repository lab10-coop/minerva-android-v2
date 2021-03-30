package minerva.android.widget.state

interface AppUIState {
    fun getAccountWidgetState(index: Int): Boolean
    fun updateAccountWidgetState(index: Int, state: Boolean)
}