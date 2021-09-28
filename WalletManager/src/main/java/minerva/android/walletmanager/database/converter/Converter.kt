package minerva.android.walletmanager.database.converter

import androidx.room.TypeConverter
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
}