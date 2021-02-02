package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class Committer(
    @SerializedName("date")
    val date: String
)