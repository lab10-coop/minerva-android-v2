package minerva.android.services.listener

import minerva.android.walletmanager.model.MinervaPrimitive

interface MinervaPrimitiveClickListener {
    fun onRemoved(minervaPrimitive: MinervaPrimitive)
    fun onContainerClick(minervaPrimitive: MinervaPrimitive)
    fun getLoggedIdentityName(minervaPrimitive: MinervaPrimitive): String
}