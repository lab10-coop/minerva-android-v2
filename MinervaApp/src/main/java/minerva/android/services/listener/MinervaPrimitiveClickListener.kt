package minerva.android.services.listener

import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive

interface MinervaPrimitiveClickListener {
    fun onRemoved(minervaPrimitive: MinervaPrimitive)
    fun onContainerClick(minervaPrimitive: MinervaPrimitive)
    fun getLoggedIdentityName(minervaPrimitive: MinervaPrimitive): String

    /**
     * On Change Account - trying to change account for existing connection
     * @param minervaPrimitive - info about existing connection
     */
    fun onChangeAccount(minervaPrimitive: MinervaPrimitive)
}