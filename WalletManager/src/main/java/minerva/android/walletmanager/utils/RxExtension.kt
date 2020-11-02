package minerva.android.walletmanager.utils

import io.reactivex.Single
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable

fun <T : Any> Single<T>.handleAutomaticBackupFailedError(pair: T) : Single<T> =
    this.onErrorResumeNext {
        if (it is HttpBadRequestException) {
            Single.error(AutomaticBackupFailedThrowable())
        } else {
            Single.just(pair)
        }
    }