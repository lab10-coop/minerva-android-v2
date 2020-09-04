package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.WalletConfig

object WalletConfigToWalletPayloadMapper : Mapper<WalletConfig, WalletConfigPayload> {
    override fun map(input: WalletConfig): WalletConfigPayload =
        WalletConfigPayload(
            input.version,
            input.identities.map { IdentityToIdentityPayloadMapper.map(it) },
            input.accounts.map { AccountToAccountPayloadMapper.map(it) },
            input.services.map { ServiceToServicePayloadMapper.map(it) },
            input.credentials.map { CredentialToCredentialPayloadMapper.map(it) }
        )
}