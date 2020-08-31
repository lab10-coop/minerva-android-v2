package minerva.android.widget

import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import minerva.android.walletmanager.model.Identity

object ProfileImage {

    fun load(imageView: ImageView, identity: Identity) {
        imageView.context.let { context ->
            identity.profileImageBitmap?.let {
                Glide.with(context)
                    .load(it)
                    .apply(RequestOptions.circleCropTransform()).into(imageView)
                return
            }
            Glide.with(context).load(LetterLogo.createLogo(context, identity.name).toBitmap()).into(imageView)
        }
    }
}