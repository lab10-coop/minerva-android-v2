package minerva.android.walletmanager.model.token

import java.math.BigDecimal

data class NativeTokenWithBalances(
    override var token: NativeToken,
    override var balance: BigDecimal,
    override val fiatBalance: BigDecimal
) : TokenWithBalances