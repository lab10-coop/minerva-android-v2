package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession

object WalletConnectSessionMapper : Mapper<WalletConnectSession, WCSession> {
    override fun map(input: WalletConnectSession): WCSession = with(input) {
        WCSession(topic, version, key, bridge, relayProtocol, relayData)
    }
}

object WCSessionToWalletConnectSessionMapper : Mapper<WCSession, WalletConnectSession> {
    override fun map(input: WCSession): WalletConnectSession = with(input) {
        WalletConnectSession(topic, version, key, bridge, relayProtocol, relayData)
    }
}

object WCPeerToWalletConnectPeerMetaMapper : Mapper<WCPeerMeta, WalletConnectPeerMeta> {
    override fun map(input: WCPeerMeta): WalletConnectPeerMeta = with(input) {
        WalletConnectPeerMeta(name, url, description, icons)
    }
}