package minerva.android.walletmanager.walletconfig

import io.reactivex.Completable
import io.reactivex.Observable
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.WalletConfigTestValues

class LocalMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfig(): Observable<WalletConfig> = Observable.just(prepareData())
    override fun saveWalletConfig(walletConfig: WalletConfig) {
    }

    fun prepareData(): WalletConfig {
        val identities = listOf(
            Identity(0, "", "", "CitizenLocal", identityData, false)
        )
        return WalletConfig(0, identities, values)
    }
}

class OnlineMock : MinervaApi, WalletConfigTestValues() {
    fun prepareResponse() = WalletConfigResponse("", "", prepareData())

    override fun getWalletConfig(content: String, publicKey: String): Observable<WalletConfigResponse> =
        Observable.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload {
        val identities = listOf(
            IdentityPayload(0, "Citizen2", identityData, false)
        )
        return WalletConfigPayload(1, identities, valuesResponse)
    }
}

class OnlineLikeLocalMock : MinervaApi, WalletConfigTestValues() {
    override fun getWalletConfig(content: String, publicKey: String): Observable<WalletConfigResponse> =
        Observable.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(content: String, publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload {
        val identities = listOf(
            IdentityPayload(0, "Citizen2", identityData, false)
        )
        return WalletConfigPayload(0, identities, valuesResponse)
    }
}