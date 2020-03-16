package minerva.android.walletmanager.manager

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import java.math.BigDecimal
import java.math.BigInteger

interface SmartContractManager {
    fun createSafeAccount(value: Value): Single<String>
    fun transferNativeCoin(network: String, transaction: Transaction): Completable
    fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable
}