package minerva.android.blockchainprovider.defs

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import minerva.android.blockchainprovider.defs.BlockchainDef.Companion.ENS

@Retention(AnnotationRetention.SOURCE)
@IntDef(ENS)
annotation class BlockchainDef {
    companion object {
        const val ENS = 303303303
    }
}