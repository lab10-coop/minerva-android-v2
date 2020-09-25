package minerva.android.widget

import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.NEW_IDENTITY_LOGO_LETTER
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.NEW_IDENTITY_PUBLIC_KEY

object ProfileImage {

    fun load(imageView: ImageView, identity: Identity) {
        imageView.context.let { context ->
            identity.profileImageBitmap?.let {
                Glide.with(context)
                    .load(it)
                    .apply(RequestOptions.circleCropTransform()).into(imageView)
                return
            }
            val logoText = if(identity.publicKey != NEW_IDENTITY_PUBLIC_KEY) identity.name
            else NEW_IDENTITY_LOGO_LETTER
            Glide.with(context).load(LetterLogo.createLogo(context, logoText).toBitmap()).into(imageView)
        }
    }
}