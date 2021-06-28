package minerva.android.walletmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.dao.TokenDao
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.token.ERC20Token

@Database(entities = [DappSessionEntity::class, ERC20Token::class], version = 10)
abstract class MinervaDatabase : RoomDatabase() {
    abstract fun dappDao(): DappSessionDao
    abstract fun tokenDao(): TokenDao
}