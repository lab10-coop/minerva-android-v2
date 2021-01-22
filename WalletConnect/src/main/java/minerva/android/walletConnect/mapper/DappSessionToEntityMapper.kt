package minerva.android.walletConnect.mapper

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletConnect.database.DappSessionEntity
import minerva.android.walletConnect.model.session.DappSession

object DappSessionToEntityMapper : Mapper<DappSession, DappSessionEntity> {
    override fun map(input: DappSession): DappSessionEntity = with(input) {
        DappSessionEntity(address, topic, version, bridge, key, name, icon, peerId, remotePeerId)
    }
}