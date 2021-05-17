package minerva.android.widget.state

import android.util.Log

class AppUIStateImpl : AppUIState {
    private var accountStates = mutableMapOf<Int, AccountWidgetState>()

    override fun getAccountWidgetState(index: Int): AccountWidgetState {
        Log.e("klop", "Current widget states with current index $index")
        accountStates.forEach {
            Log.e("klop", "${it.key}:${it.value.isWidgetOpen}")
        }
        Log.e("klop", "")
        return (accountStates[index] ?: AccountWidgetState())
    }

    override fun updateAccountWidgetState(index: Int, state: AccountWidgetState) {
        Log.e("klop", "Updating widget state. Current index ${index} with state: ${state.isWidgetOpen}")
        accountStates.forEach {
            Log.e("klop", "${it.key}:${it.value.isWidgetOpen}")
        }
        Log.e("klop", "")
        accountStates[index] = state
    }
}