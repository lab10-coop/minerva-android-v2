package minerva.android.walletmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dapp_sessions")
data class DappSessionEntity(
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "topic") val topic: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "bridge") val bridge: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String,
    @PrimaryKey @ColumnInfo(name = "peer_id") val peerId: String,
    @ColumnInfo(name = "remote_peer_id") val remotePeerId: String?
)