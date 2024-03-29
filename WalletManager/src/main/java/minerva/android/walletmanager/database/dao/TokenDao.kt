package minerva.android.walletmanager.database.dao

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.token.ERCToken

@Dao
interface TokenDao {

    @Query("SELECT * FROM tokens")
    fun getTaggedTokens(): Single<List<ERCToken>>

    @Query("SELECT * FROM tokens")
    fun getTaggedTokensFlowable(): Flowable<List<ERCToken>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tokens: List<ERCToken>)

    @Query("DELETE FROM tokens")
    fun deleteAll()

    @Transaction
    fun updateTaggedTokens(tokens: List<ERCToken>) {
        deleteAll()
        insertAll(tokens)
    }
}