package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.ATS20
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.DAI
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.FAU
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.SAI
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.SATS
import minerva.android.walletmanager.model.defs.AssetAddress.Companion.SSAI

@Retention(AnnotationRetention.SOURCE)
@StringDef(SSAI, SATS, SAI, DAI, ATS20, FAU)
annotation class AssetAddress {
    companion object {
        //ARTIS
        //main
        const val SSAI = "0x85c0935ea385e2d540f23187fd78c8a215f2e28c"
        //test
        const val SATS = "0xae9142261db90c35cd0df253abc4f53136bc80d5"
        //ETHEREUM
        //main
        const val SAI = "0x89d24a6b4ccb1b6faa2625fe562bdd9a23260359"
        const val DAI = "0x6b175474e89094c44da98b954eedeac495271d0f"
        const val ATS20 = "0xe41dd6e41f8f9962c5103d95d95f5d9b82d90fdf"
        //test
        const val FAU = "0xFab46E002BbF0b4509813474841E0716E6730136"
    }
}