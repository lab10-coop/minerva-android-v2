package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered

object WalletActionsMapper : Mapper<List<WalletActionClusteredPayload>, List<WalletActionClustered>> {

    override fun map(input: List<WalletActionClusteredPayload>): List<WalletActionClustered> {

        val actions: MutableList<WalletActionClustered> = mutableListOf()
        var walletActionsList: MutableList<WalletAction>

        input.forEach {
            walletActionsList = mutableListOf()
            it.clusteredActions.forEach { actionPayload ->
                walletActionsList.add(WalletActionMapper.map(actionPayload))
            }
            actions.add(WalletActionClustered(it.lastUsed, walletActionsList.sortedByDescending { action -> action.lastUsed }))
        }
        return actions
    }
}