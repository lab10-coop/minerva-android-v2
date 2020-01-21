package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.WalletConfigPayload

interface LocalWalletConfigProvider {
    fun loadWalletConfig(): Single<WalletConfigPayload>
    fun saveWalletConfig(walletConfig: WalletConfigPayload)
}