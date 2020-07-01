package minerva.android.widget

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.minerva_loading_view.view.*
import minerva.android.R

class MinervaLoadingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.minerva_loading_view, this)
    }

    fun startAnimation() {
        (animatedOwl.drawable as Animatable).start()
    }
}