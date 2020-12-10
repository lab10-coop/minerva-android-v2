package minerva.android.widget.repository

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.defs.NetworkShortName

//TODO downloading network icon need will be refactored
fun getNetworkIcon(context: Context, networkShort: String, isSafeAccount: Boolean = false): Drawable? =
    prepareSafeAccountBadge(context, getMainIcon(networkShort), isSafeAccount)

private fun getMainIcon(networkShort: String): Int =
    when (networkShort) {
        NetworkShortName.ATS_TAU -> R.drawable.ic_artis
        NetworkShortName.POA_SKL -> R.drawable.ic_poa
        NetworkShortName.LUKSO_14 -> R.drawable.ic_lukso
        NetworkShortName.ETH_CLASSIC_KOTTI -> R.drawable.ic_ethereum_classic
        NetworkShortName.ETH_GOR -> R.drawable.ic_gorli
        NetworkShortName.ETH_RIN, NetworkShortName.ETH_ROP, NetworkShortName.ETH_KOV -> R.drawable.ic_ethereum
        NetworkShortName.XDAI -> R.drawable.ic_xdai
        NetworkShortName.POA_CORE -> R.drawable.ic_poa_core
        NetworkShortName.ATS_SIGMA -> R.drawable.ic_artis_sigma
        else -> Int.InvalidId
    }


private fun prepareSafeAccountBadge(context: Context, mainIconRes: Int, isSafeAccount: Boolean): Drawable? {
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

private const val BADGE_INDEX = 1