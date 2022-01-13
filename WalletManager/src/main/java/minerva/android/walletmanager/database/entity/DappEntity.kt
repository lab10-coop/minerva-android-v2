package minerva.android.walletmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dapps")
data class DappEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Int = 0,
    @ColumnInfo(name = "short_name") val shortName: String,
    val subtitle: String,
    @ColumnInfo(name = "connect_link") val connectLink: String,
    @ColumnInfo(name = "button_color") val buttonColor: String,
    @ColumnInfo(name = "chain_ids") val chainIds: List<Int> = emptyList(),
    @ColumnInfo(name = "icon_link") val iconLink: String,
    @ColumnInfo(name = "long_name") val longName: String,
    @ColumnInfo(name = "explainer_title") val explainerTitle: String,
    @ColumnInfo(name = "explainer_text") val explainerText: String,
    @ColumnInfo(name = "explainer_steps") val explainerSteps: List<String> = emptyList(),
    val sponsored: Int, // 0 if not sponsored, 1 and more - sponsored order
    @ColumnInfo(name = "sponsored_chain_id") val sponsoredChainId: Int
)
