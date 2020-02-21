package minerva.android.walletmanager.manager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.manager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered
import minerva.android.walletmanager.model.mappers.configs.WalletActionsMapper
import minerva.android.walletmanager.model.mappers.payloads.WalletActionPayloadMapper
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.utils.DateUtils.isTheSameDay
import minerva.android.walletmanager.utils.PublicKeyUtils.encodePublicKey

class WalletActionsRepositoryImpl(
    private val minervaApi: MinervaApi,
    private val localWalletActionsConfigProvider: LocalWalletActionsConfigProvider
) : WalletActionsRepository {

    private var currentWalletActionsConfigVersion = Int.InvalidIndex

    override fun getWalletActions(masterKey: MasterKey): Observable<List<WalletActionClustered>> =
        Observable.mergeDelayError(
            Observable.just(localWalletActionsConfigProvider.loadWalletActionsConfig())
                .doOnNext { currentWalletActionsConfigVersion = it.version }
                .flatMap {
                    Observable.just(WalletActionsMapper.map(it.actions).sortedByDescending { action -> action.lastUsed })
                },
            minervaApi.getWalletActions(publicKey = encodePublicKey(masterKey.publicKey))
                .filter { it.walletActionsConfigPayload.version > currentWalletActionsConfigVersion }
                .doOnNext {
                    currentWalletActionsConfigVersion = it.walletActionsConfigPayload.version
                    localWalletActionsConfigProvider.saveWalletActionsConfig(it.walletActionsConfigPayload)
                }
                .flatMap { Observable.just(WalletActionsMapper.map(it.walletActionsConfigPayload.actions).sortedByDescending { action -> action.lastUsed }) }
        )

    override fun saveWalletActions(walletAction: WalletAction, masterKey: MasterKey): Completable {
        val walletActionPayload = WalletActionPayloadMapper.map(walletAction)
        var newActions: WalletActionClusteredPayload? = null

        localWalletActionsConfigProvider.loadWalletActionsConfig().run {
            if (actions.isEmpty()) {
                actions.add(WalletActionClusteredPayload(DateUtils.timestamp, mutableListOf(walletActionPayload)))
            } else {
                actions.forEach { clusteredActionPayload ->
                    if (isTheSameDay(clusteredActionPayload.lastUsed, walletActionPayload.lastUsed)) {
                        clusteredActionPayload.clusteredActions.add(walletActionPayload)
                        return updateWalletActionsConfig(this, masterKey)
                    } else {
                        newActions = WalletActionClusteredPayload(DateUtils.timestamp, mutableListOf(walletActionPayload))
                    }
                }
            }
            newActions?.let {
                actions.add(it)
            }
            return updateWalletActionsConfig(this, masterKey)
        }
    }

    private fun updateWalletActionsConfig(walletActionsConfig: WalletActionsConfigPayload, masterKey: MasterKey): Completable =
        WalletActionsConfigPayload(_version = walletActionsConfig.updateVersion, _actions = walletActionsConfig.actions).run {
            return minervaApi.saveWalletActions(walletActionsConfigPayload = this, publicKey = encodePublicKey(masterKey.publicKey))
                .doOnComplete { localWalletActionsConfigProvider.saveWalletActionsConfig(this) }
        }
}