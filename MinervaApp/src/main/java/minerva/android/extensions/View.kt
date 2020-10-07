package minerva.android.extensions

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYouListener

fun ImageView.loadImageUrl(url: String?, defaultIcon: Int) {
    if (url.isNullOrEmpty()) {
        Glide.with(context).load(defaultIcon).into(this)
        return
    }
    GlideToVectorYou.init().with(context)
        .withListener(
            object : GlideToVectorYouListener {
                override fun onLoadFailed() {
                    Handler(Looper.getMainLooper()).post {
                        Glide.with(context).load(defaultIcon).into(this@loadImageUrl)
                    }
                }
                override fun onResourceReady() { }
            })
        .setPlaceHolder(defaultIcon, defaultIcon).load(Uri.parse(url), this)
}