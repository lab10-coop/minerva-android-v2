package minerva.android.walletmanager.walletconfig

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.WalletConfigTestValues

class LocalMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfig(): Single<WalletConfigPayload> = Single.just(prepareWalletConfigPayload())
    override fun saveWalletConfig(walletConfig: WalletConfigPayload) {
    }

    private fun prepareWalletConfigPayload(): WalletConfigPayload {
        return WalletConfigPayload(1, identityResponse, valuesResponse)
    }

    fun prepareWalletConfig(): WalletConfig {
        return WalletConfig(1, identity, values)
    }
}

class OnlineMock : MinervaApi, WalletConfigTestValues() {

    override fun getWalletConfig(content: String, publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    override fun getWalletActions(content: String, publicKey: String): Observable<WalletActionsResponse> {
        TODO("not implemented")
    }

    override fun saveWalletActions(content: String, publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable {
        TODO("not implemented")
    }

    private fun prepareData(): WalletConfigPayload {
        return WalletConfigPayload(2, onlineIdentityResponse, valuesResponse)
    }

    fun prepareWalletConfig(): WalletConfig {
        return WalletConfig(2, onlineIdentity, values)
    }
}

class OnlineLikeLocalMock : MinervaApi, WalletConfigTestValues() {
    override fun getWalletConfig(content: String, publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    override fun getWalletActions(content: String, publicKey: String): Observable<WalletActionsResponse> {
        TODO("not implemented")
    }

    override fun saveWalletActions(content: String, publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable {
        TODO("not implemented")
    }

    private fun prepareData(): WalletConfigPayload {
        return WalletConfigPayload(1, identityResponse, valuesResponse)
    }

}