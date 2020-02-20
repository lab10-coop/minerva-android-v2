package minerva.android.walletmanager.utils

import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

private const val SERVICE_LAST_USED_FORMAT = "dd.MM.yyyy hh:mm"
private const val WALLET_ACTION_DATE_FORMAT = "dd.MM.yyyy"
private const val WALLET_ACTION_TIME_FORMAT = "hh:mm"

object DateUtils {

    fun getLastUsedFormatted(): String =
        SimpleDateFormat(
            SERVICE_LAST_USED_FORMAT,
            Locale.getDefault()
        ).format(Date(timestamp))


    fun getDateFromTimestamp(timestamp: Long): String =
        SimpleDateFormat(
            WALLET_ACTION_DATE_FORMAT,
            Locale.getDefault()
        ).format(Date(timestamp))

    fun getTimeFromTimeStamp(time: Long): String =
        SimpleDateFormat(
            WALLET_ACTION_TIME_FORMAT,
            Locale.getDefault()
        ).format(Date(time))

    val timestamp get() = (System.currentTimeMillis() / DateTimeConstants.MILLIS_PER_SECOND) * 1000L

    fun isTheSameDay(timestamp1: Long, timestamp2: Long) =
        DateUtils.run { getDateFromTimestamp(timestamp1) == getDateFromTimestamp(timestamp2) }

    fun isTheDayAfterTomorrow(timestamp1: Long): Boolean =
        LocalDate.now().minusDays(1) == LocalDate.fromDateFields(Date(timestamp1))
}