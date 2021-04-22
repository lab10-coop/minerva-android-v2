package minerva.android.widget.state

import android.util.Log

class AppUIStateImpl : AppUIState {
    private var accountStates = mutableMapOf<Int, AccountWidgetState>()

    override fun getAccountWidgetState(index: Int): AccountWidgetState = (accountStates[index] ?: AccountWidgetState()).apply {
        Log.e("klop", "Getting for index: $index with value: ${this.isWidgetOpen}")
    }

    override fun updateAccountWidgetState(index: Int, state: AccountWidgetState) {
        Log.e("klop", "Saving new state for index: $index with state: ${state.isWidgetOpen}")
        accountStates[index] = state
    }
}