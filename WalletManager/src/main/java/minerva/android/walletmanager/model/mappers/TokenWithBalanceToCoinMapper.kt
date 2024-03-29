package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.AssetError
import minerva.android.walletmanager.model.minervaprimitives.account.CoinCryptoBalance
import minerva.android.walletmanager.model.minervaprimitives.account.CoinError

object TokenToCoinCryptoBalanceMapper : Mapper<TokenWithBalance, CoinCryptoBalance> {
    override fun map(input: TokenWithBalance): CoinCryptoBalance = with(input) {
        CoinCryptoBalance(chainId, address, balance)
    }
}

object TokenToCoinBalanceErrorMapper : Mapper<TokenWithError, CoinError> {
    override fun map(input: TokenWithError): CoinError = with(input) {
        CoinError(chainId, address, error)
    }
}

object TokenToAssetBalanceErrorMapper {
    fun map(account: Account, input: TokenWithError): AssetError = with(input) {
        AssetError(chainId, account.privateKey, account.address, address, error, input.tokenId)
    }
}