package com.exchangemarketsprovider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class Market(
    @SerializedName("symbol")
    private val _symbol: String? = String.Empty,
    @SerializedName("price")
    private val _price: String? = String.Empty
) {
    val symbol: String get() = _symbol ?: String.Empty
    val price: String get() = _price ?: String.Empty
}