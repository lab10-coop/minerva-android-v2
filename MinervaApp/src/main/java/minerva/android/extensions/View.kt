package minerva.android.extensions

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYouListener
import minerva.android.R

fun ImageView.loadImageUrl(url: String?) {
    if (url.isNullOrEmpty()) {
        Glide.with(context).load(R.drawable.ic_default_credential).into(this)
        return
    }
    GlideToVectorYou.init().with(context)
        .withListener(
            object : GlideToVectorYouListener {
                override fun onLoadFailed() {
                    Handler(Looper.getMainLooper()).post {
                        Glide.with(context).load(R.drawable.ic_default_credential).into(this@loadImageUrl)
                    }
                }
                override fun onResourceReady() { }
            })
        .setPlaceHolder(R.drawable.ic_default_credential, R.drawable.ic_default_credential).load(Uri.parse(url), this)
}