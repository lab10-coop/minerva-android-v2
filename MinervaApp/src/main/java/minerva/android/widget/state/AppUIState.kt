package minerva.android.widget.state

interface AppUIState {

    fun getAccountUIState(index: Int): Boolean
    fun updateAccountState(index: Int, state: Boolean)

}