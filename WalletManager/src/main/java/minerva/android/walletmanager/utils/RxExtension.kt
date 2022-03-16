package minerva.android.walletmanager.utils

import io.reactivex.Completable
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.storage.LocalStorage

fun Completable.handleAutomaticBackupFailedError(localStorage: LocalStorage, onHttpBadRequestException: () -> Unit = {}): Completable =
    this.onErrorResumeNext {
        if (it is HttpBadRequestException) {
            onHttpBadRequestException()
            localStorage.isBackupAllowed = false
            Completable.error(AutomaticBackupFailedThrowable())
        } else {
            localStorage.isSynced = false
            Completable.complete()
        }
    }