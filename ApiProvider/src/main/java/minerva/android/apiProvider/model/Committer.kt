package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class Committer(
    @SerializedName("name")
    val name: String,
    @SerializedName("date")
    val date: String
)