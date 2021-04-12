package minerva.android.walletmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.entity.DappSessionEntity

@Database(entities = [DappSessionEntity::class], version = 6)
abstract class MinervaDatabase : RoomDatabase() {
    abstract fun dappDao(): DappSessionDao
}