package minerva.android.walletmanager.database.dao

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.DappSessionEntity

@Dao
interface DappSessionDao {

    @Query("SELECT * FROM dapp_sessions WHERE address = :address")
    fun getConnectedDapps(address: String): Flowable<List<DappSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dapp: DappSessionEntity): Completable

    @Query("DELETE FROM dapp_sessions WHERE peer_id = :peerId")
    fun delete(peerId: String): Completable

    @Query("SELECT * FROM dapp_sessions")
    fun getAll(): Flowable<List<DappSessionEntity>>
}