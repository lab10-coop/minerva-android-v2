package minerva.android.walletmanager.walletconfig

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.WalletConfigTestValues
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider

class LocalMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun getWalletConfig(): Single<WalletConfigPayload> = Single.just(prepareWalletConfigPayload())
    override fun saveWalletConfig(walletConfig: WalletConfigPayload) { }

    private fun prepareWalletConfigPayload(): WalletConfigPayload = WalletConfigPayload(1, identityResponse, accountsResponse)

    fun prepareWalletConfig(): WalletConfig = WalletConfig(1, identity, accounts)
}

class OnlineMock : MinervaApiRepository, WalletConfigTestValues() {

    override fun getWalletConfig(publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    override fun getWalletActions(publicKey: String): Observable<WalletActionsResponse> =
        Observable.just(WalletActionsResponse())


    override fun saveWalletActions( publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload =
        WalletConfigPayload(2, onlineIdentityResponse, accountsResponse)

    fun prepareWalletConfig(): WalletConfig =
        WalletConfig(2, onlineIdentity, accounts)
}

class OnlineLikeLocalMock : MinervaApiRepository, WalletConfigTestValues() {
    override fun getWalletConfig(publicKey: String): Single<WalletConfigResponse> =
        Single.just(WalletConfigResponse("", "", prepareData()))

    override fun saveWalletConfig(publicKey: String, walletPayload: WalletConfigPayload): Completable =
        Completable.complete()

    override fun getWalletActions(publicKey: String): Observable<WalletActionsResponse> =
        Observable.just(WalletActionsResponse())

    override fun saveWalletActions(publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable =
        Completable.complete()

    private fun prepareData(): WalletConfigPayload =
        WalletConfigPayload(1, identityResponse, accountsResponse)
}