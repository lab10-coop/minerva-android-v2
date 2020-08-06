package minerva.android.walletmanager.utils

import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig

object IdentityUtils {

    fun prepareIdentities(identity: Identity, walletConfig: WalletConfig): List<Identity> {
        val position = getPositionForIdentity(identity, walletConfig)
        walletConfig.identities.toMutableList().apply {
            if (inBounds(position)) this[position] = identity
            else add(identity)
            return this
        }
    }

    fun getPositionForIdentity(newIdentity: Identity, walletConfig: WalletConfig): Int {
        walletConfig.identities.forEachIndexed { position, identity ->
            if (newIdentity.index == identity.index) return position
        }
        return walletConfig.identities.size
    }
}