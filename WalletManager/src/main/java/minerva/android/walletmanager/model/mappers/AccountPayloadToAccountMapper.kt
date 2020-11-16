package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account

object AccountPayloadToAccountMapper {
    fun map(
        response: AccountPayload,
        publicKey: String = String.Empty,
        privateKey: String = String.Empty,
        address: String = String.Empty
    ): Account =
        Account(
            response.index,
            publicKey,
            privateKey,
            address,
            response.name,
            NetworkManager.getNetwork(response.network),
            response.isDeleted,
            owners = response.owners,
            contractAddress = response.contractAddress,
            bindedOwner = response.bindedOwner
        )
}