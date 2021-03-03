package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account

object AccountToAccountPayloadMapper : Mapper<Account, AccountPayload> {
    override fun map(input: Account): AccountPayload =
        AccountPayload(
            input.id,
            input.name,
            input.network.chainId,
            input.isDeleted,
            input.owners,
            input.contractAddress,
            input.bindedOwner
        )
}