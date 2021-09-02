package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class ERC721Details(
    @SerializedName("name")
    val name: String,
    @SerializedName("image")
    val contentUri: String,
    @SerializedName("description")
    val description: String
)
