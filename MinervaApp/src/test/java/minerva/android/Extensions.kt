package minerva.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import minerva.android.kotlinUtils.event.Event
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf

inline fun <reified T : Any?> LiveData<Event<T>>.observeLiveDataEvent(result: Event<T>) {
    val observer: Observer<Event<T>> = mock()
    val captor: KArgumentCaptor<Event<T>> = argumentCaptor()
    observeForever(observer)
    captor.run {
        verify(observer).onChanged(capture())
        firstValue shouldBeInstanceOf Event::class
        firstValue.peekContent() shouldBeEqualTo result.peekContent()
    }
    verifyNoMoreInteractions(observer)
}

inline fun <reified T : Any> LiveData<T>.observeWithPredicate(predicate: (T) -> Boolean) {
    val observer: Observer<T> = mock()
    val captor: KArgumentCaptor<T> = argumentCaptor()
    observeForever(observer)
    captor.run {
        verify(observer).onChanged(capture())
        predicate(lastValue) shouldBeEqualTo true
    }
}