package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class AccessTokenPayload(
    @SerializedName("access_token")
    var jwtToken: String
)