package minerva.android.walletmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_dapps")
data class FavoriteDappEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Int = 0,
    val name: String // dapps has distinctive short name
)
