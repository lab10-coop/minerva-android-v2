package minerva.android.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("profile")
    var profile: Profile
)