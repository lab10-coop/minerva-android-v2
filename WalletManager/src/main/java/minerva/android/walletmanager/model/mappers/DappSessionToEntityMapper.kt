package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.walletconnect.DappSessionV1

object DappSessionToEntityMapper : Mapper<DappSessionV1, DappSessionEntity> {
    override fun map(input: DappSessionV1): DappSessionEntity = with(input) {
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
            chainId,
            handshakeId,
            isMobileWalletConnect
        )
    }
}