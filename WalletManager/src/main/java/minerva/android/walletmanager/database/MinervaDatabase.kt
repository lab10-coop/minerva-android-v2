package minerva.android.walletmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import minerva.android.walletmanager.database.converter.Converter
import minerva.android.walletmanager.database.converters.TokenTypeConverter
import minerva.android.walletmanager.database.dao.CoinBalanceDao
import minerva.android.walletmanager.database.dao.DappDao
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.dao.TokenBalanceDao
import minerva.android.walletmanager.database.dao.TokenDao
import minerva.android.walletmanager.database.entity.CoinBalanceEntity
import minerva.android.walletmanager.database.entity.DappEntity
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.database.entity.TokenBalanceEntity
import minerva.android.walletmanager.model.token.ERCToken

@Database(
    entities = [ERCToken::class, DappSessionEntity::class, CoinBalanceEntity::class, TokenBalanceEntity::class, DappEntity::class],
    version = 26
)
@TypeConverters(TokenTypeConverter::class, Converter::class)
abstract class MinervaDatabase : RoomDatabase() {
    abstract fun dappSessionDao(): DappSessionDao
    abstract fun tokenDao(): TokenDao
    abstract fun coinBalanceDao(): CoinBalanceDao
    abstract fun tokenBalanceDao(): TokenBalanceDao
    abstract fun dappDao(): DappDao
}