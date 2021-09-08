package minerva.android.walletmanager.model.token

import java.math.BigDecimal

data class NativeTokenWithBalances(
    override var token: NativeToken,
    override var currentBalance: BigDecimal,
    override val fiatBalance: BigDecimal
) : TokenWithBalances