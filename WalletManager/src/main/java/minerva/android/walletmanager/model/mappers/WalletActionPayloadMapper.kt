package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.WalletAction

object WalletActionPayloadMapper : Mapper<WalletAction, WalletActionPayload> {
    override fun map(input: WalletAction): WalletActionPayload =
        WalletActionPayload(
            _type = input.type,
            _status = input.status,
            _lastUsed = input.lastUsed,
            _fields = input.fields
        )
}