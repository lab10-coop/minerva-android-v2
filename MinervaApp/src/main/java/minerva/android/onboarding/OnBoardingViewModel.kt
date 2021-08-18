package minerva.android.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import minerva.android.kotlinUtils.event.Event

class OnBoardingViewModel : ViewModel() {

    private val _updateActionBar = MutableLiveData<Event<Unit>>()
    val updateActionBar: LiveData<Event<Unit>> get() = _updateActionBar


    fun updateActionBar() {
        _updateActionBar.value = Event(Unit)
    }
}