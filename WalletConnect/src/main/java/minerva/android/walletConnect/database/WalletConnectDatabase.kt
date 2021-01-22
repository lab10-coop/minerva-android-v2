package minerva.android.walletConnect.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DappSessionEntity::class], version = 1)
abstract class WalletConnectDatabase : RoomDatabase() {
    abstract fun dappDao(): DappSessionDao
}