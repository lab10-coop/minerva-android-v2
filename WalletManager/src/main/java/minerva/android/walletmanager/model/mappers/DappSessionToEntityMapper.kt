package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.walletconnect.DappSession

object DappSessionToEntityMapper : Mapper<DappSession, DappSessionEntity> {
    override fun map(input: DappSession): DappSessionEntity = with(input) {
        DappSessionEntity(
            address,
            topic,
            version,
            bridge,
            key,
            name,
            iconUrl,
            peerId,
            remotePeerId,
            networkName,
            accountName,
            networkShort
        )
    }
}