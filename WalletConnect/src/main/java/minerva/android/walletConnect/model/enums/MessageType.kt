package minerva.android.walletConnect.model.enums

import com.google.gson.annotations.SerializedName

enum class MessageType {
    @SerializedName("pub") PUB,
    @SerializedName("sub") SUB
}