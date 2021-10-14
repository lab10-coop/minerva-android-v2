package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class TokenTx(
    @SerializedName("blockNumber")
    val blockNumber: String = String.Empty,
    @SerializedName("timeStamp")
    val timestamp: String = String.Empty,
    @SerializedName("hash")
    val hash: String = String.Empty,
    @SerializedName("nonce")
    val nonce: String = String.Empty,
    @SerializedName("blockHash")
    val blockHash: String = String.Empty,
    @SerializedName("from")
    val from: String = String.Empty,
    @SerializedName("contractAddress")
    val address: String = String.Empty,
    @SerializedName("to")
    val to: String = String.Empty,
    @SerializedName("value")
    val value: String = String.Empty,
    @SerializedName("tokenName")
    val tokenName: String = String.Empty,
    @SerializedName("tokenSymbol")
    val tokenSymbol: String = String.Empty,
    @SerializedName("tokenDecimal")
    val tokenDecimal: String = String.Empty,
    @SerializedName("transactionIndex")
    val transactionIndex: String = String.Empty,
    @SerializedName("gas")
    val gas: String = String.Empty,
    @SerializedName("gasPrice")
    val gasPrice: String = String.Empty,
    @SerializedName("gasUsed")
    val gasUsed: String = String.Empty,
    @SerializedName("cumulativeGasUsed")
    val cumulativeGasUsed: String = String.Empty,
    @SerializedName("input")
    val input: String = String.Empty,
    @SerializedName("confirmations")
    val confirmations: String = String.Empty,
    @SerializedName("tokenID")
    val tokenId: String = String.Empty
)
