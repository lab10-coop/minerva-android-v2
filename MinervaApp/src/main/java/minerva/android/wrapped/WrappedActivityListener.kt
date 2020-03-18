package minerva.android.wrapped

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import minerva.android.kotlinUtils.event.Event

interface WrappedActivityListener {

    val extraStringLiveData: LiveData<Event<String>>
    fun putStringExtra(string: String)
    fun goBack(fragment: Fragment)
    fun showScanner()
}