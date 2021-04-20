package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidValue
import kotlin.reflect.full.memberProperties

data class FiatPrice(
    @SerializedName("eur")
    val eur: Double? = Double.InvalidValue,
    @SerializedName("usd")
    val usd: Double? = Double.InvalidValue,
    @SerializedName("gbp")
    val gbp: Double? = Double.InvalidValue,
    @SerializedName("aed")
    val aed: Double? = Double.InvalidValue,
    @SerializedName("ars")
    val ars: Double? = Double.InvalidValue,
    @SerializedName("aud")
    val aud: Double? = Double.InvalidValue,
    @SerializedName("bdt")
    val bdt: Double? = Double.InvalidValue,
    @SerializedName("bhd")
    val bhd: Double? = Double.InvalidValue,
    @SerializedName("bmd")
    val bmd: Double? = Double.InvalidValue,
    @SerializedName("brl")
    val brl: Double? = Double.InvalidValue,
    @SerializedName("cad")
    val cad: Double? = Double.InvalidValue,
    @SerializedName("chf")
    val chf: Double? = Double.InvalidValue,
    @SerializedName("clp")
    val clp: Double? = Double.InvalidValue,
    @SerializedName("cny")
    val cny: Double? = Double.InvalidValue,
    @SerializedName("czk")
    val czk: Double? = Double.InvalidValue,
    @SerializedName("dkk")
    val dkk: Double? = Double.InvalidValue,
    @SerializedName("hkd")
    val hkd: Double? = Double.InvalidValue,
    @SerializedName("huf")
    val huf: Double? = Double.InvalidValue,
    @SerializedName("idr")
    val idr: Double? = Double.InvalidValue,
    @SerializedName("ils")
    val ils: Double? = Double.InvalidValue,
    @SerializedName("inr")
    val inr: Double? = Double.InvalidValue,
    @SerializedName("jpy")
    val jpy: Double? = Double.InvalidValue,
    @SerializedName("krw")
    val krw: Double? = Double.InvalidValue,
    @SerializedName("kwd")
    val kwd: Double? = Double.InvalidValue,
    @SerializedName("lkr")
    val lkr: Double? = Double.InvalidValue,
    @SerializedName("mmk")
    val mmk: Double? = Double.InvalidValue,
    @SerializedName("mxn")
    val mxn: Double? = Double.InvalidValue,
    @SerializedName("myr")
    val myr: Double? = Double.InvalidValue,
    @SerializedName("ngn")
    val ngn: Double? = Double.InvalidValue,
    @SerializedName("nok")
    val nok: Double? = Double.InvalidValue,
    @SerializedName("nzd")
    val nzd: Double? = Double.InvalidValue,
    @SerializedName("php")
    val php: Double? = Double.InvalidValue,
    @SerializedName("pkr")
    val pkr: Double? = Double.InvalidValue,
    @SerializedName("pln")
    val pln: Double? = Double.InvalidValue,
    @SerializedName("rub")
    val rub: Double? = Double.InvalidValue,
    @SerializedName("sar")
    val sar: Double? = Double.InvalidValue,
    @SerializedName("sek")
    val sek: Double? = Double.InvalidValue,
    @SerializedName("sgd")
    val sgd: Double? = Double.InvalidValue,
    @SerializedName("thb")
    val thb: Double? = Double.InvalidValue,
    @SerializedName("try")
    val trY: Double? = Double.InvalidValue,
    @SerializedName("twd")
    val twd: Double? = Double.InvalidValue,
    @SerializedName("uah")
    val uah: Double? = Double.InvalidValue,
    @SerializedName("vef")
    val vef: Double? = Double.InvalidValue,
    @SerializedName("vnd")
    val vnd: Double? = Double.InvalidValue,
    @SerializedName("xdr")
    val xdr: Double? = Double.InvalidValue,
    @SerializedName("zar")
    val zar: Double? = Double.InvalidValue
) {

    fun getRate(fiat: String): Double = getFiatPrices()[fiat] ?: Double.InvalidValue

    private fun getFiatPrices(): Map<String, Double> =
        mutableMapOf<String, Double>().apply {
            this@FiatPrice::class.memberProperties.forEach {
                put(it.name.toUpperCase(), it.getter.call(this@FiatPrice) as Double)
            }
        }
}
