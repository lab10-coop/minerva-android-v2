package minerva.android.values.akm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.values.ValueManager
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.smartContract.SmartContractRepository
import timber.log.Timber

class SafeAccountSettingsViewModel(
    private val valueManager: ValueManager,
    private val smartContractRepository: SmartContractRepository
) : BaseViewModel() {

    internal lateinit var value: Value
    private var masterOwnerPrivateKey: String = String.Empty

    private val _ownersLiveData = MutableLiveData<List<String>>()
    val ownersLiveData: LiveData<List<String>> get() = _ownersLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun loadValue(index: Int) {
        value = valueManager.loadValue(index)
        _ownersLiveData.value = value.owners?.reversed()
        getOwners(value.address, value.network, value.privateKey)
        value.masterOwnerAddress.let {
            smartContractRepository.getSafeAccountMasterOwnerPrivateKey(it).apply {
                masterOwnerPrivateKey = this
            }
        }
    }

    fun addOwner(owner: String) {
        if (isOwnerAlreadyAdded(owner)) {
            _errorLiveData.value = Event(Throwable("Error: Owner already added!"))
            return
        }
        launchDisposable {
            smartContractRepository.addSafeAccountOwner(owner, value.address, value.network, masterOwnerPrivateKey, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = {
                        Timber.e("Owners list download error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun removeOwner(removeAddress: String) {
        if (isMasterOwner(removeAddress)) {
            _errorLiveData.value = Event(Throwable("Error: Cannot remove masterOwner Address!"))
            return
        }
        if (!isOwnerAlreadyAdded(removeAddress)) {
            _errorLiveData.value = Event(Throwable("Error: No address owner on the list!"))
            return
        }

        launchDisposable {
            smartContractRepository.removeSafeAccountOwner(removeAddress, value.address, value.network, masterOwnerPrivateKey, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = {
                        Timber.e("Owners list download error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun isOwnerAlreadyAdded(owner: String): Boolean {
        value.owners?.forEach {
            if (it == owner) return true
        }
        return false
    }

    private fun isMasterOwner(removeAddress: String) = value.masterOwnerAddress == removeAddress

    @VisibleForTesting
    fun getOwners(contractAddress: String, network: String, privateKey: String) {
        launchDisposable {
            smartContractRepository.getSafeAccountOwners(contractAddress, network, privateKey, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = { Timber.e("Owners list download error: ${it.message}") }
                )
        }
    }

    val valueName get() = value.name
}