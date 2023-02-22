package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession

object WalletConnectSessionMapper : Mapper<WalletConnectSession, WCSession> {
    override fun map(input: WalletConnectSession): WCSession = with(input) {
        WCSession(topic, version, bridge, key)
    }
}

object WCSessionToWalletConnectSessionMapper : Mapper<WCSession, WalletConnectSession> {
    override fun map(input: WCSession): WalletConnectSession = with(input) {
        WalletConnectSession(topic, version, bridge, key)
    }
}

object WCPeerToWalletConnectPeerMetaMapper : Mapper<WCPeerMeta, WalletConnectPeerMeta> {
    override fun map(input: WCPeerMeta): WalletConnectPeerMeta = with(input) {
        WalletConnectPeerMeta(name, url, description, icons, chainId = input.chainId, peerId = input.peerId)
    }
}