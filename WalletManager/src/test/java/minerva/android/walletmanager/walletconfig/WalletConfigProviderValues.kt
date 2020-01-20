package minerva.android.walletmanager.walletconfig

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.WalletConfigTestValues

class LocalMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfig(): Observable<WalletConfig> = Observable.just(prepareData())
    override fun saveWalletConfig(walletConfig: WalletConfig) {}
    fun prepareData(): WalletConfig {
        return WalletConfig(0, identities, values, services)
    }
}

class OnlineLikeLocalMock : MinervaApi, WalletConfigTestValues() {
    override fun getWalletConfig(content: String, publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletConfigPayload: WalletConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload {
        return WalletConfigPayload(0, identityResponses, valuesResponse, serviceResponse)
    }
}

class OnlineMock : MinervaApi, WalletConfigTestValues() {
    fun prepareResponse() = WalletConfigResponse("", "", prepareData())

    override fun getWalletConfig(content: String, publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload {
        return WalletConfigPayload(1, identityResponses, valuesResponse, serviceResponse)
    }
}

class LocalLikeOnlineMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfig(): Observable<WalletConfig> = Observable.just(prepareData())
    override fun saveWalletConfig(walletConfig: WalletConfig) {}
    fun prepareData(): WalletConfig {
        return WalletConfig(0, identities, values, services)
    }
}