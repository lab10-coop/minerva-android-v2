package minerva.android.walletmanager.database.converters

import androidx.room.TypeConverter
import minerva.android.walletmanager.model.token.TokenType

class TokenTypeConverter {
    @TypeConverter
    fun fromTokenType(tokenType: TokenType): String = tokenType.name

    @TypeConverter
    fun toTokenType(tokenType: String): TokenType = TokenType.valueOf(tokenType)
}
