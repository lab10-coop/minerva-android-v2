package minerva.android.walletmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "coin_balances")
data class CoinBalanceEntity(
    @PrimaryKey val balanceHash: String,
    val address: String,
    val chainId: Int,
    @ColumnInfo(name = "crypto_balance") val cryptoBalance: BigDecimal,
    @ColumnInfo(name = "fiat_balance") val fiatBalance: BigDecimal,
    val rate: Double
)