package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.CoinBalanceEntity

@Dao
interface CoinBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coinBalance: CoinBalanceEntity): Completable

    @Query("SELECT * FROM coin_balances WHERE address = :address AND chainId = :chainId")
    fun getCoinBalance(address: String, chainId: Int): Single<CoinBalanceEntity>
}