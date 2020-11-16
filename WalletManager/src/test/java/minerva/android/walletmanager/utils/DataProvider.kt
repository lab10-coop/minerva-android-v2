package minerva.android.walletmanager.utils

import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.WalletConfigTestValues.accountsResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.identityResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.onlineIdentityResponse
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.defs.NetworkShortName

object DataProvider {

    val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    val localWalletConfigPayload = WalletConfigPayload(_version = 1, _identityPayloads = identityResponse, _accountPayloads = accountsResponse)

    val onlineWalletConfigResponse =
        WalletConfigPayload(_version = 0, _identityPayloads = onlineIdentityResponse, _accountPayloads = accountsResponse)

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "11", "privateKey", "address1", data),
            Identity(1, "identityName2", "", "privateKey", "address2", data),
            Identity(3, "identityName3", "", "privateKey", "address3", data)
        ),
        listOf(
            Account(2, "publicKey1", "privateKey1", "address", network = Network(short = NetworkShortName.ETH_RIN)),
            Account(4, "publicKey2", "privateKey2", "address", network = Network(short = NetworkShortName.ATS_TAU)),
            Account(
                5, "publicKey3", "privateKey3", "address", network = Network(short = NetworkShortName.ATS_TAU),
                owners = listOf("masterOwner")
            ),
            Account(
                6, "publicKey4", "privateKey4", "address", network = Network(short = NetworkShortName.ATS_TAU),
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Account(7, "publicKey5", "privateKey5", "address", network = Network(short = NetworkShortName.ATS_TAU))
        ), listOf(
            Service("1", "name")
        ),
        listOf(
            Credential(
                loggedInIdentityDid = "did:ethr:address",
                type = CredentialType.VERIFIABLE_CREDENTIAL.type,
                membershipType = CredentialType.AUTOMOTIVE_CLUB.type,
                issuer = "iss"
            )
        )
    )
}