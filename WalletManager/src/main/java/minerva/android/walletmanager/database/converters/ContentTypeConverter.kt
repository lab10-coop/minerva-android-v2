package minerva.android.walletmanager.database.converters

import androidx.room.TypeConverter
import minerva.android.walletmanager.model.ContentType

class ContentTypeConverter {
    @TypeConverter
    fun fromContentType(contentType: ContentType): String = contentType.name

    @TypeConverter
    fun toContentType(contentType: String): ContentType = ContentType.valueOf(contentType)
}
