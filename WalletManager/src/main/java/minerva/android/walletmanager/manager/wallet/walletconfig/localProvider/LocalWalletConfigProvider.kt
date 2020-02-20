package minerva.android.walletmanager.manager.wallet.walletconfig.localProvider

import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload

interface LocalWalletConfigProvider {
    fun loadWalletConfig(): Single<WalletConfigPayload>
    fun saveWalletConfig(walletConfig: WalletConfigPayload)
}