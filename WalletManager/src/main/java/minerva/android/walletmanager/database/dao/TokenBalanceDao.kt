package minerva.android.walletmanager.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.database.entity.TokenBalanceEntity

@Dao
interface TokenBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokenBalance: TokenBalanceEntity): Completable

    @Query("SELECT * FROM token_balances WHERE address = :address AND account_address = :accountAddress")
    fun getTokenBalance(address: String, accountAddress: String): Single<TokenBalanceEntity>
}