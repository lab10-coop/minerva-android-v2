package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class TokenPayload(
    @SerializedName("access_token")
    var jwtToken: String
)