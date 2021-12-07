package minerva.android.accounts.extensions

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import java.math.BigDecimal

fun MutableList<AccountToken>.findCachedAccountToken(accountToken: AccountToken): AccountToken? =
    find { cachedAccountToken ->
        cachedAccountToken.isTheSameToken(accountToken.token.address, accountToken.token.accountAddress, accountToken.token.tokenId)
    }

fun AccountToken.isTheSameToken(tokenAddress: String, accountAddress: String, tokenId: String?): Boolean =
    token.address.equals(tokenAddress, true) && token.accountAddress.equals(accountAddress, true) &&
            token.tokenId == tokenId

fun List<ERCToken>.findCachedToken(balance: AssetError): ERCToken? =
    find { token ->
        token.address.equals(balance.tokenAddress, true) &&
                token.accountAddress.equals(balance.accountAddress, true)
    }

fun AccountToken.isTokenShown(account: Account): Boolean =
    account.accountTokens.find { accountToken ->
        accountToken.isTheSameToken(token.address, token.accountAddress, token.tokenId)
    } != null

fun MutableList<AccountToken>.filterDistinctAccountTokensForGivenAccount(account: Account): MutableList<AccountToken> =
    filter { accountToken ->
        accountToken.token.accountAddress.equals(account.address, true) &&
                accountToken.token.chainId == account.chainId
    }.distinctBy{ kotlin.Pair(it.token.address, it.token.tokenId) }
        .toMutableList()

fun Account.shouldUpdateCoinBalance(coinBalance: CoinBalance): Boolean =
    isAccountToUpdate(coinBalance) && (cryptoBalance != coinBalance.balance.cryptoBalance ||
            fiatBalance != coinBalance.balance.fiatBalance)

fun Account.isAccountToUpdate(coinBalance: Coin): Boolean =
    (address.equals(coinBalance.address, true) && chainId == coinBalance.chainId)

fun AccountToken.shouldShowCachedToken(
    account: Account,
    tokenVisibilitySettings: TokenVisibilitySettings
): Boolean =
    token.accountAddress.equals(account.address, true) &&
            token.chainId == account.chainId &&
            tokenVisibilitySettings.getTokenVisibility(account.address, token.address) == true

fun AccountToken.shouldShowAccountToken(
    account: Account,
    tokenVisibilitySettings: TokenVisibilitySettings
): Boolean =
    tokenVisibilitySettings.getTokenVisibility(
        account.address,
        token.address
    ) == true && (currentBalance.hasFunds() || token.isError)

fun BigDecimal.hasFunds() = this > BigDecimal.ZERO

fun AccountToken.shouldUpdateBalance(balance: AssetBalance): Boolean =
    currentRawBalance != balance.accountToken.currentRawBalance || tokenPrice != balance.accountToken.tokenPrice

fun AccountToken.isTokenError(balance: AssetError) =
    isTheSameToken(balance.tokenAddress, balance.accountAddress, balance.tokenId) && !token.isError

fun Asset.isTokenInAccount(account: Account): Boolean =
    chainId == account.chainId && privateKey.equals(account.privateKey, true)

fun AccountToken.setNewTokenPrice(
    balance: AssetBalance,
    cachedAccountTokens: MutableList<AccountToken>
) {
    tokenPrice = if (balance.accountToken.tokenPrice == Double.InvalidValue) {
        val cachedAccountToken =
            cachedAccountTokens.findCachedAccountToken(balance.accountToken)
        cachedAccountToken?.tokenPrice
    } else {
        balance.accountToken.tokenPrice
    }
}