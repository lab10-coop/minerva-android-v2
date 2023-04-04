package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import minerva.android.R
import minerva.android.databinding.DappDialogButtonsBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.gone

abstract class DappDialog(context: Context, val approve: () -> Unit = {}, val deny: () -> Unit = {}) :
    BottomSheetDialog(context) {

    abstract val networkHeader: DappNetworkHeaderBinding

    init {
        this.setCancelable(false)
        setupDialog()
    }

    fun initButtons(confirmationButtons: DappDialogButtonsBinding) = with(confirmationButtons) {
        cancel.setOnClickListener {
            deny()
            dismiss()
        }
        confirm.setOnClickListener {
            approve()
            dismiss()
        }

        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                deny()
                dismiss()
            }
            true
        }
    }

    /**
     * Setup Header
     * @param dappName
     * @param networkName
     * @param icon
     * @param ssb - coolest(!) string with mixed (colors, fonts, size) part of it
     */
    fun setupHeader(dappName: String, networkName: String?, icon: Any, ssb: SpannableStringBuilder? = null) = with(networkHeader) {
        name.text = dappName
        if (networkName == null || networkName.isEmpty()) {
            if (null == ssb) {
                network.gone()
            } else {
                network.text = ssb
            }
        } else {
            network.text = networkName
        }

        Glide.with(context)
            .load(icon)
            .error(R.drawable.ic_services)
            .into(networkHeader.icon)
    }

    private fun setupDialog() = with(this.behavior) {
        setOnShowListener {
            skipCollapsed
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    ViewCompat.setBackground(bottomSheet, createMaterialShapeDrawable(bottomSheet))
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { /*do nothing*/
            }
        })
    }

    fun createMaterialShapeDrawable(bottomSheet: View): MaterialShapeDrawable {
        val shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, 0, R.style.CustomShapeAppearanceBottomSheetDialog)
            .build()
        val currentMaterialShapeDrawable = bottomSheet.background as MaterialShapeDrawable
        return MaterialShapeDrawable(shapeAppearanceModel).apply {
            initializeElevationOverlay(context)
            fillColor = ContextCompat.getColorStateList(context, R.color.white)
            elevation = currentMaterialShapeDrawable.elevation
        }
    }
}