package minerva.android.widget.state

interface AppUIState {

    fun getAccountState(index: Int): Boolean
    fun updateAccountState(index: Int, state: Boolean)

}