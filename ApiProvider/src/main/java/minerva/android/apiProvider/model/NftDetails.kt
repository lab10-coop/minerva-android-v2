package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class NftDetails(
    @SerializedName("name")
    val name: String,
    @SerializedName("image", alternate = ["image_url"])
    val imageUri: String,
    @SerializedName("animation_url")
    val  animationUrl: String?,
    @SerializedName("description")
    val description: String
)
