package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.BuildConfig
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.WalletConfig

object WalletConfigToWalletPayloadMapper : Mapper<WalletConfig, WalletConfigPayload> {
    override fun map(input: WalletConfig): WalletConfigPayload =
        WalletConfigPayload(
            _version = input.version,
            modelVersion = BuildConfig.MODEL_VERSION,
            _identityPayloads = input.identities.map { IdentityToIdentityPayloadMapper.map(it) },
            _accountPayloads = input.accounts.map { AccountToAccountPayloadMapper.map(it) },
            _servicesPayloads = input.services.map { ServiceToServicePayloadMapper.map(it) },
            _credentialPayloads = input.credentials.map { CredentialToCredentialPayloadMapper.map(it) },
            _erc20Tokens = input.erc20Tokens.map { (key, value) -> key to value.map { ERC20TokenToERC20TokenPayloadMapper.map(it) } }
                .toMap()
        )
}