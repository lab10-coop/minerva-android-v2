package minerva.android.walletmanager.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes
import minerva.android.walletmanager.utils.CryptoUtils

interface WalletConfigRepository {
    fun loadWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig>
    fun getWalletConfig(masterSeed: MasterSeed): Single<WalletConfigResponse>
    fun updateWalletConfig(masterSeed: MasterSeed, walletConfigPayload: WalletConfigPayload = createDefaultWalletConfig()): Completable
    fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload = createDefaultWalletConfig())
    fun createWalletConfig(masterSeed: MasterSeed): Completable
}

fun createDefaultWalletConfig() =
    WalletConfigPayload(
        DefaultWalletConfigIndexes.DEFAULT_VERSION,
        listOf(IdentityPayload(DefaultWalletConfigIndexes.FIRST_IDENTITY_INDEX, DefaultWalletConfigFields.DEFAULT_IDENTITY_NAME)),
        listOf(
            AccountPayload(
                DefaultWalletConfigIndexes.FIRST_ACCOUNTS_INDEX,
                CryptoUtils.prepareName(Network.ATS_TAU, DefaultWalletConfigIndexes.FIRST_ACCOUNTS_INDEX),
                Network.ATS_TAU.short
            ),
            AccountPayload(
                DefaultWalletConfigIndexes.SECOND_ACCOUNTS_INDEX,
                CryptoUtils.prepareName(Network.ETH_RIN, DefaultWalletConfigIndexes.SECOND_ACCOUNTS_INDEX),
                Network.ETH_RIN.short
            )
        )
    )