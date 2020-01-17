package minerva.android.walletmanager.utils

import org.joda.time.DateTimeConstants
import java.text.SimpleDateFormat
import java.util.*

private const val SERVICE_LAST_USED_FORMAT = "dd.MM.yyyy hh:mm"

object DateUtils {

    fun getLastUsed(): String =
        SimpleDateFormat(
            SERVICE_LAST_USED_FORMAT,
            Locale.getDefault()
        ).format(Date((System.currentTimeMillis() / DateTimeConstants.MILLIS_PER_SECOND) * 1000L))
}