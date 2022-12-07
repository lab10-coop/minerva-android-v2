package minerva.android.mock

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import java.math.BigDecimal

val accounts = listOf(
    Account(1, chainId = 2, address = "account0"),
    Account(2, chainId = 1, address = "account1"),
    Account(3, chainId = 1, address = "account2"),
    Account(4, chainId = 3, address = "address3")
)
val accountsWithoutPrimaryAccount = listOf(
    Account(1, chainId = 2),
    Account(4, chainId = 3),
    Account(4, chainId = 2)
)

val networks = listOf(
    Network(httpRpc = "some_rpc", chainId = 1),
    Network(httpRpc = "some_rpc", chainId = 2),
    Network(httpRpc = "some_rpc", chainId = 3)
)

val accountTokensForPrivateKey1 = listOf(
    AccountToken(
        rawBalance = BigDecimal.TEN,
        token = ERCToken(
            1,
            "name",
            accountAddress = "address1",
            address = "tokenAddress1",
            decimals = "18",
            type = TokenType.ERC20
        )
    ),
    AccountToken(
        rawBalance = BigDecimal.TEN,
        token = ERCToken(
            1,
            "name",
            accountAddress = "address1",
            address = "tokenAddress2",
            decimals = "18",
            type = TokenType.ERC20
        )
    )
)

val accountTokensForPrivateKey2 = listOf(
    AccountToken(
        rawBalance = BigDecimal.TEN,
        token = ERCToken(
            2,
            "name",
            accountAddress = "address2",
            address = "tokenAddress3",
            decimals = "18",
            type = TokenType.ERC20
        )
    ),
    AccountToken(
        rawBalance = BigDecimal.TEN,
        token = ERCToken(
            2,
            "name",
            accountAddress = "address2",
            address = "tokenAddress4",
            decimals = "18",
            type = TokenType.ERC20
        )
    )
)