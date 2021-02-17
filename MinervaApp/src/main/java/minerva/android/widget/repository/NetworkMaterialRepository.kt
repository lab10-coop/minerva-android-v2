package minerva.android.widget.repository

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.XDAI

//TODO downloading network icon need will be refactored
fun getNetworkIcon(context: Context, networkShort: String, isSafeAccount: Boolean = false): Drawable? =
    prepareSafeAccountBadge(context, getMainIcon(networkShort), isSafeAccount)

private fun getMainIcon(networkShort: String): Int =
    when (networkShort) {
        ATS_SIGMA -> R.drawable.ic_artis_sigma
        ETH_MAIN -> R.drawable.ic_ethereum
        XDAI -> R.drawable.ic_xdai
        POA_CORE -> R.drawable.ic_poa_core
        ATS_TAU -> R.drawable.ic_artis
        POA_SKL -> R.drawable.ic_poa
        LUKSO_14 -> R.drawable.ic_lukso
        ETH_KOV, ETH_RIN, ETH_ROP -> R.drawable.ic_etherium_test
        ETH_GOR -> R.drawable.ic_gorli
        else -> Int.InvalidId
    }

fun getMainTokenIconRes(networkShort: String): Int =
    when (networkShort) {
        ATS_SIGMA -> R.drawable.ic_artis_sigma_token
        ETH_MAIN -> R.drawable.ic_ethereum_token
        XDAI -> R.drawable.ic_xdai_token
        POA_CORE -> R.drawable.ic_poa_token
        ATS_TAU -> R.drawable.ic_artis_token
        ETH_KOV, ETH_RIN, ETH_ROP, ETH_GOR -> R.drawable.ic_ethereum_token_test
        POA_SKL -> R.drawable.ic_skl_token
        LUKSO_14 -> R.drawable.ic_lukso
        else -> R.drawable.ic_default_token
    }

private fun prepareSafeAccountBadge(context: Context, mainIconRes: Int, isSafeAccount: Boolean)
        : Drawable
? {
    ContextCompat.getDrawable(context, mainIconRes)?.let { mainIcon ->
        ContextCompat.getDrawable(context, R.drawable.ic_safe_account)?.let { safeBadge ->
            return if (!isSafeAccount) mainIcon
            else LayerDrawable(arrayOf(mainIcon, safeBadge)).apply {
                setLayerGravity(BADGE_INDEX, Gravity.TOP or Gravity.END)
            }
        }
    }
    return null
}

private const
val BADGE_INDEX = 1