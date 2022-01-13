package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.FavoriteDappEntity

@Dao
interface FavoriteDappDao {

    @Query("SELECT * FROM favorite_dapps")
    fun getAllFavoriteDapps(): Single<List<FavoriteDappEntity>>

    @Query("DELETE FROM favorite_dapps WHERE name NOT IN (:names)")
    fun deleteNotMatchingDapps(names: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(dapps: List<FavoriteDappEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dapp: FavoriteDappEntity): Completable

    @Query("DELETE FROM favorite_dapps WHERE name = :name")
    fun delete(name: String): Completable

    @Query("DELETE FROM favorite_dapps")
    fun deleteAll()
}