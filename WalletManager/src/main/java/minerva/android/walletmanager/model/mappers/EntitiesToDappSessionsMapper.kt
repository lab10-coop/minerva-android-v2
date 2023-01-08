package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.walletconnect.DappSessionV1

object EntitiesToDappSessionsMapper : Mapper<List<DappSessionEntity>, List<DappSessionV1>> {
    override fun map(input: List<DappSessionEntity>): List<DappSessionV1> =
        mutableListOf<DappSessionV1>().apply {
            input.forEach { entity ->
                with(entity) {
                    this@apply.add(
                        DappSessionV1(
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
                            chainId,
                            handshakeId,
                            isMobileWalletConnect
                        )
                    )
                }
            }
        }
}

object SessionEntityToDappSessionMapper : Mapper<DappSessionEntity, DappSessionV1> {
    override fun map(input: DappSessionEntity): DappSessionV1 = with(input) {
        DappSessionV1(
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
            chainId,
            handshakeId,
            isMobileWalletConnect
        )
    }

}