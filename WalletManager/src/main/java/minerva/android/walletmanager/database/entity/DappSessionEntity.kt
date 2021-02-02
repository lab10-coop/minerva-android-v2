package minerva.android.walletmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import minerva.android.kotlinUtils.Empty

@Entity(tableName = "dapp_sessions")
data class DappSessionEntity(
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "topic") val topic: String = String.Empty,
    @ColumnInfo(name = "version") val version: String = String.Empty,
    @ColumnInfo(name = "bridge") val bridge: String = String.Empty,
    @ColumnInfo(name = "key") val key: String = String.Empty,
    @ColumnInfo(name = "name") val name: String = String.Empty,
    @ColumnInfo(name = "icon") val icon: String = String.Empty,
    @PrimaryKey @ColumnInfo(name = "peer_id") val peerId: String = String.Empty,
    @ColumnInfo(name = "remote_peer_id") val remotePeerId: String? = String.Empty,
    @ColumnInfo(name = "network_name") val networkName: String = String.Empty,
    @ColumnInfo(name = "account_name") val accountName: String = String.Empty
)