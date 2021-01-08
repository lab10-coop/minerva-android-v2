package minerva.android.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AddAssetViewModel(private val transactionRepository: TransactionRepository) : BaseViewModel() {

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address)

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _addressDetailsLiveData = MutableLiveData<Asset>()
    val addressDetailsLiveData: LiveData<Asset> get() = _addressDetailsLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    //TODO getting asset details is only mock. Will be implemented in one of the next task
    fun getAssetDetails(address: String) =
        launchDisposable {
            Single.just(getAssetDetailsMock(address))
                .delay(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = { _addressDetailsLiveData.value = it },
                    onError = {
                        Timber.e("Checking Asset details error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }

    //TODO getting asset details is only mock. Will be implemented in one of the next task
    @Deprecated("This method is only mock - getting asset details is not implemented yet")
    private fun getAssetDetailsMock(address: String): Asset =
        if (address == "0xE3C915f7D9aB8Ee2c045dD31cc9121512D72e988")
            Asset("WOOPWOOP", "WOOP", "0xE3C915f7D9aB8Ee2c045dD31cc9121512D72e988", "18")
        else Asset()
}