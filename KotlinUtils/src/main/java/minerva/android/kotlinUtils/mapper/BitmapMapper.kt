package minerva.android.kotlinUtils.mapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import minerva.android.kotlinUtils.NO_DATA
import java.io.ByteArrayOutputStream

object BitmapMapper {

    private const val PROFILE_IMAGE_QUALITY = 90

    fun toBase64(bitmap: Bitmap, quality: Int = PROFILE_IMAGE_QUALITY): String {
        ByteArrayOutputStream().let { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
            return String(Base64.encode(stream.toByteArray(), Base64.DEFAULT))
        }
    }

    fun fromBase64(base64: String): Bitmap? {
        if (base64 == String.NO_DATA) return null
        Base64.decode(base64, Base64.DEFAULT).let {
            return BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }
}