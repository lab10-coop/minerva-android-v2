package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.DappSession

object EntityToDappSessionMapper : Mapper<List<DappSessionEntity>, List<DappSession>> {
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
                            accountName
                        )
                    )
                }
            }
        }
}