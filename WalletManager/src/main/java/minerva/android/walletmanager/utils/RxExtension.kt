package minerva.android.walletmanager.utils

import io.reactivex.Single
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.storage.LocalStorage
import timber.log.Timber

fun <T : Any> Single<T>.handleAutomaticBackupFailedError(pair: T, localStorage: LocalStorage): Single<T> =
    this.onErrorResumeNext {
        if (it is HttpBadRequestException) {
            localStorage.isBackupAllowed = false
            Single.error(AutomaticBackupFailedThrowable())
        } else {
            localStorage.isSynced = false
            Single.just(pair)
        }
    }