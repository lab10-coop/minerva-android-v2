package minerva.android.walletmanager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.DateUtils.isTheSameDay
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered
import minerva.android.walletmanager.model.mappers.WalletActionPayloadMapper
import minerva.android.walletmanager.model.mappers.WalletActionsMapper
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProvider
import timber.log.Timber

class WalletActionsRepositoryImpl(
    private val minervaApi: MinervaApi,
    private val localWalletActionsConfigProvider: LocalWalletActionsConfigProvider,
    private val walletConfigManager: WalletConfigManager
) : WalletActionsRepository {

    private var currentWalletActionsConfigVersion = Int.InvalidIndex

    override fun getWalletActions(): Observable<List<WalletActionClustered>> =
        Observable.mergeDelayError(
            Observable.just(localWalletActionsConfigProvider.loadWalletActionsConfig())
                .doOnNext { currentWalletActionsConfigVersion = it.version }
                .flatMap { Observable.just(WalletActionsMapper.map(it.actions).sortedByDescending { action -> action.lastUsed }) },
            minervaApi.getWalletActions(publicKey = encodePublicKey(walletConfigManager.masterSeed.publicKey))
                .filter { it.walletActionsConfigPayload.version > currentWalletActionsConfigVersion }
                .doOnNext {
                    currentWalletActionsConfigVersion = it.walletActionsConfigPayload.version
                    localWalletActionsConfigProvider.saveWalletActionsConfig(it.walletActionsConfigPayload)
                }
                .flatMap {
                    Observable.just(
                        WalletActionsMapper.map(it.walletActionsConfigPayload.actions).sortedByDescending { action -> action.lastUsed })
                }
        ).onErrorResumeNext { _: Observer<in List<WalletActionClustered>> ->
            Observable.just(localWalletActionsConfigProvider.loadWalletActionsConfig())
                .flatMap { Observable.just(WalletActionsMapper.map(it.actions).sortedByDescending { action -> action.lastUsed }) }

        }

    override fun saveWalletActions(walletActions: List<WalletAction>): Completable {
        Timber.tag("kobe").d("save wallet action")

        val payloads: MutableList<WalletActionPayload> = mutableListOf()
        walletActions.forEach {
            payloads.add(WalletActionPayloadMapper.map(it))
        }

        var newActions: WalletActionClusteredPayload? = null

        localWalletActionsConfigProvider.loadWalletActionsConfig().run {
            if (actions.isEmpty()) {
                actions.add(WalletActionClusteredPayload(DateUtils.timestamp, payloads))
            } else {
                actions.forEach { clusteredActionPayload ->
                    if (isTheSameDay(clusteredActionPayload.lastUsed, payloads.first().lastUsed)) {
                        clusteredActionPayload.clusteredActions.addAll(payloads)
                        return updateWalletActionsConfig(this, walletConfigManager.masterSeed)
                    } else {
                        newActions = WalletActionClusteredPayload(DateUtils.timestamp, payloads)
                    }
                }
            }
            newActions?.let {
                actions.add(it)
            }
            return updateWalletActionsConfig(this, walletConfigManager.masterSeed)
        }
    }

    private fun updateWalletActionsConfig(walletActionsConfig: WalletActionsConfigPayload, masterSeed: MasterSeed): Completable =
        minervaApi.saveWalletActions(walletActionsConfigPayload = walletActionsConfig, publicKey = encodePublicKey(masterSeed.publicKey))
            .doOnTerminate {
                Timber.tag("kobe").d("save wallet action on terminate")
                localWalletActionsConfigProvider.saveWalletActionsConfig(walletActionsConfig)
            }
            .onErrorComplete()
}