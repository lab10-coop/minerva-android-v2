package minerva.android.walletmanager.database.dao

import androidx.room.*
import io.reactivex.Single
import minerva.android.walletmanager.model.token.ERC20Token

@Dao
interface TokenDao {

    @Query("SELECT * FROM tokens")
    fun getTaggedTokens(): Single<List<ERC20Token>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tokens: List<ERC20Token>)

    @Query("DELETE FROM tokens")
    fun deleteAll()

    @Transaction
    fun updateTokens(tokens: List<ERC20Token>) {
        deleteAll()
        insertAll(tokens)
    }
}