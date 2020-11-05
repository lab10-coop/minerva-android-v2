package minerva.android.walletmanager.walletconfig.localProvider

import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.utils.DefaultWalletConfig

interface LocalWalletConfigProvider {
    fun getWalletConfig(): Single<WalletConfigPayload>
    fun saveWalletConfig(payload: WalletConfigPayload = DefaultWalletConfig.create)
}