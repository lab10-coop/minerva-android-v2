package minerva.android.walletmanager.utils

import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.configProvider.BuildConfig
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_MAIN_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FOURTH_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_DEFAULT_MAIN_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.THIRD_DEFAULT_NETWORK_INDEX

object DefaultWalletConfig {

    private val firstDefaultTestNetwork = NetworkManager.firstDefaultValueNetwork()
    private val secondDefaultTestNetwork = NetworkManager.getNetworkByIndex(SECOND_DEFAULT_NETWORK_INDEX)
    private val firstDefaultMainNetwork = NetworkManager.getNetworkByIndex(THIRD_DEFAULT_NETWORK_INDEX)
    private val secondDefaultMainNetwork = NetworkManager.getNetworkByIndex(FOURTH_DEFAULT_NETWORK_INDEX)

    val create: WalletConfigPayload
        get() =
            WalletConfigPayload(
                _version = DefaultWalletConfigIndexes.DEFAULT_VERSION,
                modelVersion = BuildConfig.MODEL_VERSION,
                _identityPayloads = listOf(IdentityPayload(FIRST_IDENTITY_INDEX, DEFAULT_IDENTITY_NAME)),
                _accountPayloads = listOf(
                    AccountPayload(
                        FIRST_DEFAULT_NETWORK_INDEX,
                        CryptoUtils.prepareName(firstDefaultTestNetwork.name, FIRST_DEFAULT_NETWORK_INDEX),
                        firstDefaultTestNetwork.chainId
                    ),
                    AccountPayload(
                        SECOND_DEFAULT_NETWORK_INDEX,
                        CryptoUtils.prepareName(secondDefaultTestNetwork.name, SECOND_DEFAULT_NETWORK_INDEX),
                        secondDefaultTestNetwork.chainId
                    ),
                    AccountPayload(
                        FIRST_DEFAULT_MAIN_NETWORK_INDEX,
                        CryptoUtils.prepareName(firstDefaultMainNetwork.name, FIRST_DEFAULT_MAIN_NETWORK_INDEX),
                        firstDefaultMainNetwork.chainId
                    ),
                    AccountPayload(
                        SECOND_DEFAULT_MAIN_NETWORK_INDEX,
                        CryptoUtils.prepareName(secondDefaultMainNetwork.name, SECOND_DEFAULT_MAIN_NETWORK_INDEX),
                        secondDefaultMainNetwork.chainId
                    )
                )
            )
}