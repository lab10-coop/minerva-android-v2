package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.BuildConfig
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.wallet.WalletConfig

object WalletConfigToWalletPayloadMapper : Mapper<WalletConfig, WalletConfigPayload> {
    override fun map(input: WalletConfig): WalletConfigPayload =
        WalletConfigPayload(
            _version = input.version,
            modelVersion = BuildConfig.MODEL_VERSION,
            _identityPayloads = input.identities.map { identity -> IdentityToIdentityPayloadMapper.map(identity) },
            _accountPayloads = input.accounts.map { account -> AccountToAccountPayloadMapper.map(account) },
            _servicesPayloads = input.services.map { service -> ServiceToServicePayloadMapper.map(service) },
            _credentialPayloads = input.credentials.map { credential -> CredentialToCredentialPayloadMapper.map(credential) },
            _erc20Tokens = input.erc20Tokens.map { (key, value) ->
                /*Needed to remove NFT from wallet config, where we store only ERC-20 tokens, comment can be removed when compatibility with NFT is implemented*/
                key to value.filter { erC20Token -> erC20Token.decimals.isNotEmpty() }
                    .map { erC20Token -> ERC20TokenToERC20TokenPayloadMapper.map(erC20Token) }
            }.toMap()
        )
}