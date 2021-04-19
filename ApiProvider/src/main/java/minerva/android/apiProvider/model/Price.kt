package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidValue

data class Price(
    @SerializedName("eur")
    val eur: Double? = Double.InvalidValue,
    @SerializedName("usd")
    val usd: Double? = Double.InvalidValue,
    @SerializedName("gbp")
    val gbp: Double? = Double.InvalidValue
//    @SerializedName("aed")
//    val aed: Double? = Double.InvalidValue,
//    @SerializedName("ars")
//    val ars: Double? = Double.InvalidValue,
//    @SerializedName("aud")
//    val aud: Double? = Double.InvalidValue,
//    @SerializedName("bch")
//    val bch: Double? = Double.InvalidValue,
//    @SerializedName("bdt")
//    val bdt: Double? = Double.InvalidValue,
//    @SerializedName("bhd")
//    val bhd: Double? = Double.InvalidValue,
//    @SerializedName("bmd")
//    val bmd: Double? = Double.InvalidValue,
//    @SerializedName("bnb")
//    val bnb: Double? = Double.InvalidValue,
//    @SerializedName("brl")
//    val brl: Double? = Double.InvalidValue,
//    @SerializedName("btc")
//    val btc: Double? = Double.InvalidValue,

//TODO klop add currences
//"aed":
//"ars": 51457,
//"aud": 717.2,
//"bch": 0.59941022,
//"bdt": 47042,
//"bhd": 209.21,
//"bmd": 554.97,
//"bnb": 1.066223,
//"brl": 3117.04,
//"btc": 0.00894337,
//"cad": 695.68,
//"chf": 511.74,
//"clp": 387644,
//"cny": 3623.26,
//"czk": 12033.32,
//"dkk": 3449.41,
//"dot": 13.035592,
//"eos": 70.126,
//"eth": 0.22636391,
//"hkd": 4311.31,
//"huf": 166780,
//"idr": 8083660,
//"ils": 1820.44,
//"inr": 41427,
//"jpy": 60391,
//"krw": 618943,
//"kwd": 167.33,
//"lkr": 111509,
//"ltc": 1.919419,
//"mmk": 782227,
//"mxn": 11072.02,
//"myr": 2290.34,
//"ngn": 211442,
//"nok": 4656.75,
//"nzd": 774.91,
//"php": 26833,
//"pkr": 84742,
//"pln": 2111.56,
//"rub": 42343,
//"sar": 2081.66,
//"sek": 4691.74,
//"sgd": 740.8,
//"thb": 17350.45,
//"try": 4458.32,
//"twd": 15718.02,
//"uah": 15512.91,
//"vef": 55.57,
//"vnd": 12801730,
//"xag": 21.45,
//"xau": 0.314466,
//"xdr": 388.51,
//"xlm": 896.53,
//"xrp": 324.892,
//"yfi": 0.01126205,
//"zar": 7875.99,
//"bits": 8943.37,
//"link": 13.410627,
//"sats": 894337
)