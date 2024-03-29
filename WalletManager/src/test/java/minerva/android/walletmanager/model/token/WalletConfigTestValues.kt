package minerva.android.walletmanager.model.token

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network

object WalletConfigTestValues {

    val identityData: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991",
        "Some Key" to "Some value",
        "Some Key 2" to "Some value",
        "Some Key 3" to "Some value",
        "Some Key 4" to "Some value"
    )

    val identityResponse = listOf(
        IdentityPayload(0, "IdentityName1", identityData, false)
    )

    val onlineIdentityResponse = listOf(
        IdentityPayload(0, "OnlineIdentityName1", identityData, false)
    )

    val identities = listOf(
        Identity(2, "pubicKey", "privateKey", "IdentityName1", "address", identityData, false),
        Identity(3, "pubicKey", "privateKey", "IdentityName2", "address", identityData, true)
    )

    val tokens =
        mapOf(
            Pair(
                1, listOf(
                    ERCToken(1, "CookieCoin", "COOKiE", "0xC00k13", "13", type = TokenType.ERC20),
                    ERCToken(2, "otherCoin", "OC", "0x0th3rC01n", "32", type = TokenType.ERC20)
                )
            ),
            Pair(
                2, listOf(
                    ERCToken(2, "diffCoin", "DiFF", "0xD1FF", "13", type = TokenType.ERC20)
                )
            )
        )


    val networks = listOf(
        Network(chainId = 246785),
        Network(chainId = 4)
    )

    val accounts = listOf(
        Account(1, "publicKey", "privateKey", "address", "ValuePayload1", networks[0].chainId, contractAddress = "test"),
        Account(2, "publicKey", "privateKey", "address", "ValuePayload2", networks[1].chainId)
    )

    val accountsResponse = listOf(
        AccountPayload(1, "ValuePayload1", networks[0].chainId, false, _contractAddress = "contractAddress", _isTestNetwork = true),
        AccountPayload(2, "ValuePayload2", networks[1].chainId, false, _contractAddress = "contractAddress", _isTestNetwork = true)
    )
}
