package minerva.android.model

import com.google.gson.annotations.SerializedName

data class TokenPayload(
    @SerializedName("token")
    var jwtToken: String
)