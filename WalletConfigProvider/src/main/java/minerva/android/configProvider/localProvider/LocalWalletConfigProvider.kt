package minerva.android.configProvider.localProvider

import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload

interface LocalWalletConfigProvider {
    fun getWalletConfig(): Single<WalletConfigPayload>
    fun saveWalletConfig(payload: WalletConfigPayload): Single<WalletConfigPayload>
}