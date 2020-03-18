package minerva.android.values.akm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.model.Value
import timber.log.Timber

class SafeAccountSettingsViewModel(private val walletManager: WalletManager, private val smartContractManager: SmartContractManager) :
    BaseViewModel() {

    private lateinit var value: Value
    private var masterOwnerPrivateKey: String = String.Empty

    private val _ownersLiveData = MutableLiveData<List<String>>()
    val ownersLiveData: LiveData<List<String>> get() = _ownersLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun loadValue(index: Int) {
        value = walletManager.loadValue(index)
        _ownersLiveData.value = value.owners?.reversed()
        getOwners(value.address, value.network, value.privateKey)
        value.owners?.last().let {
            walletManager.getSafeAccountMasterOwnerPrivateKey(it).apply {
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
                smartContractManager.addSafeAccountOwner(owner, value.address, value.network, masterOwnerPrivateKey)
                    .andThen(walletManager.updateSafeAccountOwners(value.index, prepareOwnerList(owner)))
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
            if(it == owner) return true
        }
        return false
    }

    @VisibleForTesting
    fun getOwners(contractAddress: String, network: String, privateKey: String) {
        launchDisposable {
            smartContractManager.getSafeAccountOwners(contractAddress, network, privateKey)
                .flatMap { walletManager.updateSafeAccountOwners(value.index, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = { Timber.e("Owners list download error: ${it.message}") }
                )
        }
    }

    private fun prepareOwnerList(owner: String): List<String> {
        value.owners?.toMutableList()?.let {
            it.add(FIRST_POSITION, owner)
            return it
        }
        throw IllegalStateException("Owners Live Data was not initialized")
    }

    fun removeOwner(position: Int) {
        //TODO implement removing owner
    }

    companion object {
        private const val FIRST_POSITION = 0
    }
}