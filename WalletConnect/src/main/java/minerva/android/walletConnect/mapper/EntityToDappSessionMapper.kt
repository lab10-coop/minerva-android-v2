package minerva.android.walletConnect.mapper

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletConnect.database.DappSessionEntity
import minerva.android.walletConnect.model.session.DappSession

object EntityToDappSessionMapper : Mapper<List<DappSessionEntity>, List<DappSession>> {

    override fun map(input: List<DappSessionEntity>): List<DappSession> {
        val list: MutableList<DappSession> = mutableListOf()
        input.forEach { entity ->
            with(entity) {
                list.add(
                    DappSession(
                        address,
                        topic,
                        version,
                        bridge,
                        key,
                        name,
                        icon,
                        peerId,
                        remotePeerId
                    )
                )
            }
        }
        return list
    }
}