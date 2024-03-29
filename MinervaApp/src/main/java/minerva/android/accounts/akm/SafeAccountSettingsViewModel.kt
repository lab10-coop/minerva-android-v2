package minerva.android.accounts.akm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.AlreadyRemovedOwnerThrowable
import minerva.android.walletmanager.exception.CannotRemoveMasterOwnerAddressThrowable
import minerva.android.walletmanager.exception.OwnerAlreadyAddedThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import timber.log.Timber

class SafeAccountSettingsViewModel(
    private val accountManager: AccountManager,
    private val safeAccountRepository: SafeAccountRepository
) : BaseViewModel() {

    internal lateinit var account: Account
    private var masterOwnerPrivateKey: String = String.Empty

    private val _ownersLiveData = MutableLiveData<List<String>>()
    val ownersLiveData: LiveData<List<String>> get() = _ownersLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun isAddressValid(address: String) =
        accountManager.isAddressValid(address)

    fun loadAccount(index: Int) {
        account = accountManager.loadAccount(index)
        _ownersLiveData.value = account.owners?.reversed()
        getOwners(account.address, account.network.chainId, account.privateKey)

        val masterOwnerAddress = account.masterOwnerAddress
        safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(masterOwnerAddress).apply {
            masterOwnerPrivateKey = this
        }
    }

    fun addOwner(owner: String) {
        if (isOwnerAlreadyAdded(owner)) {
            _errorLiveData.value = Event(OwnerAlreadyAddedThrowable())
            return
        }
        launchDisposable {
            safeAccountRepository.addSafeAccountOwner(
                owner,
                account.address,
                account.network.chainId,
                masterOwnerPrivateKey,
                account
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = {
                        Timber.e("Add owner error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun removeOwner(removeAddress: String) {
        if (isMasterOwner(removeAddress)) {
            _errorLiveData.value = Event(CannotRemoveMasterOwnerAddressThrowable())
            return
        }
        if (!isOwnerAlreadyAdded(removeAddress)) {
            _errorLiveData.value = Event(AlreadyRemovedOwnerThrowable())
            return
        }

        launchDisposable {
            safeAccountRepository.removeSafeAccountOwner(
                removeAddress,
                account.address,
                account.network.chainId,
                masterOwnerPrivateKey,
                account
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = {
                        Timber.e("Remove owner error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun isOwnerAlreadyAdded(owner: String): Boolean {
        account.owners?.forEach {
            if (it == owner) return true
        }
        return false
    }

    private fun isMasterOwner(removeAddress: String) = account.masterOwnerAddress == removeAddress

    @VisibleForTesting
    fun getOwners(contractAddress: String, chainId: Int, privateKey: String) {
        launchDisposable {
            safeAccountRepository.getSafeAccountOwners(
                contractAddress,
                chainId,
                privateKey,
                account
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _ownersLiveData.value = it.reversed() },
                    onError = { Timber.e("Owners list download error: ${it.message}") }
                )
        }
    }

    val accountName get() = account.name
}