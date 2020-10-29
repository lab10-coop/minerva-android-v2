package minerva.android.walletmanager.walletconfig.localProvider

import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload

interface LocalWalletConfigProvider {
    fun getWalletConfig(): Single<WalletConfigPayload>
    fun saveWalletConfig(walletConfig: WalletConfigPayload)
}