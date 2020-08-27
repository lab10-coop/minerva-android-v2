package minerva.android.services.listener

import minerva.android.walletmanager.model.MinervaPrimitive

interface MinervaPrimitiveMenuListener {
    fun onRemoved(minervaPrimitive: MinervaPrimitive)
}