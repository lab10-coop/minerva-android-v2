package minerva.android.extensions

import android.net.Uri
import android.widget.ImageView
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import minerva.android.R

fun ImageView.loadImageUrl(url: String?) {
    GlideToVectorYou.init().with(context).setPlaceHolder(-1, R.drawable.ic_minerva_icon).load(Uri.parse(url), this)
}