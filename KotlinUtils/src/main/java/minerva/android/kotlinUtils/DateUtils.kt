package minerva.android.kotlinUtils

import minerva.android.kotlinUtils.extension.toMilliseconds
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    const val DATE_FORMAT = "dd.MM.yyyy"
    const val SHORT_DATE_FORMAT = "MM/yyyy"
    private const val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssX"
    private const val DATE_WITH_TIME_FORMAT = "dd.MM.yyyy hh:mm"
    private const val TIME_FORMAT = "hh:mm"

    fun getDateWithTimeFromTimestamp(dateInMillis: Long = timestamp): String =
        SimpleDateFormat(
            DATE_WITH_TIME_FORMAT,
            Locale.getDefault()
        ).format(dateInMillis)

    fun getDateFromTimestamp(timestamp: Long, format: String = DATE_FORMAT): String =
        SimpleDateFormat(
            format,
            Locale.getDefault()
        ).format(timestamp.toMilliseconds())

    fun getTimeFromTimeStamp(time: Long = timestamp): String =
        SimpleDateFormat(
            TIME_FORMAT,
            Locale.getDefault()
        ).format(time)

    fun getTimestampFromDate(date: String, format: String = ISO_8601): Long =
        SimpleDateFormat(format).parse(date).time

    val timestamp get() = (System.currentTimeMillis() / DateTimeConstants.MILLIS_PER_SECOND) * 1000L

    fun isTheSameDay(timestamp1: Long, timestamp2: Long) =
        DateUtils.run { getDateFromTimestamp(timestamp1, DATE_FORMAT) == getDateFromTimestamp(timestamp2, DATE_FORMAT) }

    fun isTheDayAfterTomorrow(timestamp1: Long): Boolean =
        LocalDate.now().minusDays(1) == LocalDate.fromDateFields(Date(timestamp1))
}
