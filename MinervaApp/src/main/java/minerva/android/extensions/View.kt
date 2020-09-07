package minerva.android.extensions

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import minerva.android.R

fun ImageView.loadImageUrl(url: String?) {
    if (url.isNullOrEmpty()) {
        Glide.with(context).load(R.drawable.ic_default_credential).into(this)
        return
    }
    GlideToVectorYou.init().with(context).setPlaceHolder(-1, R.drawable.ic_default_credential).load(Uri.parse(url), this)
}