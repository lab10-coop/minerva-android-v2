package minerva.android.walletmanager.utils

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes

object DefaultWalletConfig {
    val create: WalletConfigPayload
        get() =
            WalletConfigPayload(
                DefaultWalletConfigIndexes.DEFAULT_VERSION,
                listOf(IdentityPayload(DefaultWalletConfigIndexes.FIRST_IDENTITY_INDEX, DefaultWalletConfigFields.DEFAULT_IDENTITY_NAME)),
                listOf(
                    with(NetworkManager.firstDefaultValueNetwork()) {
                        AccountPayload(
                            DefaultWalletConfigIndexes.FIRST_ACCOUNTS_INDEX,
                            CryptoUtils.prepareName(this, DefaultWalletConfigIndexes.FIRST_ACCOUNTS_INDEX),
                            short
                        )
                    },
                    with(NetworkManager.secondDefaultValueNetwork()) {
                        AccountPayload(
                            DefaultWalletConfigIndexes.SECOND_ACCOUNTS_INDEX,
                            CryptoUtils.prepareName(this, DefaultWalletConfigIndexes.SECOND_ACCOUNTS_INDEX),
                            short
                        )
                    }
                )
            )
}