package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.wallet.WalletAction

object WalletActionMapper : Mapper<WalletActionPayload, WalletAction> {
    override fun map(input: WalletActionPayload): WalletAction =
        WalletAction(
            type = input.type,
            status = input.status,
            lastUsed = input.lastUsed,
            fields = input.fields
        )
}