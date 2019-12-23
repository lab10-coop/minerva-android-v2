package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import minerva.android.walletmanager.model.WalletConfig

interface LocalWalletConfigProvider {
    fun loadWalletConfig(): Observable<WalletConfig>
    fun saveWalletConfig(walletConfig: WalletConfig)
}