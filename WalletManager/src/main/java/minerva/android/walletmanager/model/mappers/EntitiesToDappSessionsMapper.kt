package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.walletconnect.DappSession

object EntitiesToDappSessionsMapper : Mapper<List<DappSessionEntity>, List<DappSession>> {
    override fun map(input: List<DappSessionEntity>): List<DappSession> =
        mutableListOf<DappSession>().apply {
            input.forEach { entity ->
                with(entity) {
                    this@apply.add(
                        DappSession(
                            address,
                            topic,
                            version,
                            bridge,
                            key,
                            name,
                            icon,
                            peerId,
                            remotePeerId,
                            networkName,
                            accountName,
                            chainId
                        )
                    )
                }
            }
        }
}

object SessionEntityToDappSessionMapper : Mapper<DappSessionEntity, DappSession> {
    override fun map(input: DappSessionEntity): DappSession = with(input) {
        DappSession(address, topic, version, bridge, key, name, icon, peerId, remotePeerId, networkName, accountName, chainId)
    }

}