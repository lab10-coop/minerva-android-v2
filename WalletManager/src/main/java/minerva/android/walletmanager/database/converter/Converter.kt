package minerva.android.walletmanager.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.math.BigDecimal
import java.math.BigInteger

class Converter {
    @TypeConverter
    fun bigIntegerToString(value: BigInteger): String = value.toString()

    @TypeConverter
    fun stringToBigInteger(value: String): BigInteger = BigInteger(value)

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String): BigDecimal = BigDecimal(value)

    @TypeConverter
    fun stringListToJson(value: List<String>) = Gson().toJson(value)

    @TypeConverter
    fun jsonToStringList(value: String) = Gson().fromJson(value, Array<String>::class.java).toList()


    @TypeConverter
    fun intListToJson(value: List<Int>) = Gson().toJson(value)

    @TypeConverter
    fun jsonToIntList(value: String) = Gson().fromJson(value, Array<Int>::class.java).toList()
}
