package minerva.android.walletmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import minerva.android.walletmanager.database.converter.Converter
import minerva.android.walletmanager.database.dao.CoinBalanceDao
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.dao.TokenBalanceDao
import minerva.android.walletmanager.database.dao.TokenDao
import minerva.android.walletmanager.database.entity.CoinBalanceEntity
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.database.entity.TokenBalanceEntity
import minerva.android.walletmanager.model.token.ERC20Token

@Database(
    entities = [DappSessionEntity::class, ERC20Token::class, CoinBalanceEntity::class, TokenBalanceEntity::class],
    version = 22
)
@TypeConverters(Converter::class)
abstract class MinervaDatabase : RoomDatabase() {
    abstract fun dappDao(): DappSessionDao
    abstract fun tokenDao(): TokenDao
    abstract fun coinBalanceDao(): CoinBalanceDao
    abstract fun tokenBalanceDao(): TokenBalanceDao
}