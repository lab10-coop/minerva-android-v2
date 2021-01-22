package minerva.android.walletConnect.database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable

@Dao
interface DappSessionDao {

    @Query("SELECT * FROM dapp_sessions WHERE address = :address")
    fun getConnectedDapps(address: String): Flowable<List<DappSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dapp: DappSessionEntity): Completable

    @Query("DELETE FROM dapp_sessions WHERE peer_id = :peerId")
    fun delete(peerId: String): Completable
}