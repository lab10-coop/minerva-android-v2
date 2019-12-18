package minerva.android.walletmanager.walletconfig

import io.reactivex.Single

interface OnlineWalletConfigProvider {
    fun loadWalletConfigRaw(): Single<String>
}