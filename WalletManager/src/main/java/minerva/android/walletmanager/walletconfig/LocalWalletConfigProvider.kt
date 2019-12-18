package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable

interface LocalWalletConfigProvider {
    fun loadWalletConfigRaw(): Observable<String>
}