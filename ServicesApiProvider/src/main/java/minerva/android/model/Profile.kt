package minerva.android.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class Profile (
    @SerializedName("did")
    var did: String? = String.Empty
)