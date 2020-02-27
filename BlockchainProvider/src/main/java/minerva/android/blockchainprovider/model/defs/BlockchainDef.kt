package minerva.android.blockchainprovider.model.defs

import androidx.annotation.StringDef
import minerva.android.blockchainprovider.model.defs.BlockchainDef.Companion.ENS

@Retention(AnnotationRetention.SOURCE)
@StringDef(ENS)
annotation class BlockchainDef {
    companion object {
        const val ENS = "ENS"
    }
}