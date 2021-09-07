package minerva.android.walletmanager.database.converter

import androidx.room.TypeConverter
import java.math.BigInteger

class BigIntegerConverter {
    @TypeConverter
    fun bigIntegerToString(value: BigInteger): String = value.toString()

    @TypeConverter
    fun stringToBigInteger(value: String): BigInteger = BigInteger(value)
}