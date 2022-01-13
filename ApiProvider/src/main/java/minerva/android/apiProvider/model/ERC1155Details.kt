package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class ERC1155Details(
    @SerializedName("name")
    val name: String,
    @SerializedName("image", alternate = ["image_url"])
    val contentUri: String,
    @SerializedName("description")
    val description: String
)
