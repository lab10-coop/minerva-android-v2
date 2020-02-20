package minerva.android.walletmanager.manager.walletActions.localProvider

import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload

interface LocalWalletActionsConfigProvider {
    fun loadWalletActionsConfig(): WalletActionsConfigPayload
    fun saveWalletActionsConfig(walletActionsConfigPayload: WalletActionsConfigPayload)
}