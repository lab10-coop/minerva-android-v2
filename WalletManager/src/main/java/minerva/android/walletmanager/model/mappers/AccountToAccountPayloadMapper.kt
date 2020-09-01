package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Account

object AccountToAccountPayloadMapper : Mapper<Account, AccountPayload> {
    override fun map(input: Account): AccountPayload =
        AccountPayload(
            input.index,
            input.name,
            input.network,
            input.isDeleted,
            input.owners,
            input.contractAddress,
            input.bindedOwner
        )
}