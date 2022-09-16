package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.DappSessionEntity

@Dao
interface DappSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dapp: DappSessionEntity): Completable

    /**
     * Update - update db record with info of current wallet connection
     * @param peerId - id of socket_client connection
     * @param address - address of account which was specified
     * @param chainId - chainId of account which was specified
     * @param accountName - name (with index) of account which was specified
     * @return Completable
     */
    @Query("UPDATE dapp_sessions SET address = :address, account_name = :accountName, chain_id = :chainId WHERE peer_id = :peerId")
    fun update(peerId: String, address: String, chainId: Int,  accountName: String): Completable

    @Query("DELETE FROM dapp_sessions WHERE peer_id = :itemId")
    fun delete(itemId: String): Completable

    @Query("DELETE FROM dapp_sessions WHERE address = :key")
    fun deleteAllDappsForAccount(key: String): Completable

    @Query("SELECT * FROM dapp_sessions")
    fun getAll(): Flowable<List<DappSessionEntity>>

    @Query("SELECT * FROM dapp_sessions WHERE peer_id = :itemId")
    fun getDappSessionById(itemId: String): Single<DappSessionEntity>
}