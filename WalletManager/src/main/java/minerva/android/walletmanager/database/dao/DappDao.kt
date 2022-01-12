package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.DappEntity

@Dao
interface DappDao {
    @Query("SELECT * FROM dapps")
    fun getAllDapps(): Single<List<DappEntity>>

    @Query("SELECT * FROM dapps WHERE sponsored_chain_id = :chainId")
    fun getSponsoredDappForChainId(chainId: Int): Single<DappEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(dapps: List<DappEntity>)

    @Query("DELETE FROM dapps")
    fun deleteAll()
}
