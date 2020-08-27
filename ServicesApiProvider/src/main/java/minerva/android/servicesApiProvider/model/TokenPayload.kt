package minerva.android.servicesApiProvider.model

import com.google.gson.annotations.SerializedName

data class TokenPayload(
    @SerializedName("access_token")
    var jwtToken: String
)