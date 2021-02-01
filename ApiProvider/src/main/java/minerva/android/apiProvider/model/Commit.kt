package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class Commit(
    @SerializedName("committer")
    val committer: Committer
)