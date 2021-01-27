package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.walletmanager.database.entity.DappSessionEntity

@Dao
interface DappSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dapp: DappSessionEntity): Completable

    @Query("DELETE FROM dapp_sessions WHERE peer_id = :peerId")
    fun delete(peerId: String): Completable

    @Query("DELETE FROM dapp_sessions WHERE address = :key")
    fun deleteAllDappsForAccount(key: String): Completable

    @Query("SELECT * FROM dapp_sessions")
    fun getAll(): Flowable<List<DappSessionEntity>>
}