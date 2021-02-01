package minerva.android.kotlinUtils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `Check getting date with time from Timestamp` () {
        val timestamp = 1612193618000
        val result = DateUtils.getDateWithTimeFromTimestamp(timestamp)
        result shouldBeEqualTo "01.02.2021 04:33"
    }

    @Test
    fun `Check getting date from timestamp` () {
        val timestamp = 1612193618000
        val result = DateUtils.getDateFromTimestamp(timestamp)
        result shouldBeEqualTo "01.02.2021"
    }

    @Test
    fun `Check getting time from timestamp` () {
        val timestamp = 1612193618000
        val result = DateUtils.getTimeFromTimeStamp(timestamp)
        result shouldBeEqualTo "04:33"
    }

    @Test
    fun `Check formatting date from string to mills` () {
        val date = "2021-01-29T19:56:02Z"
        val result = DateUtils.getTimestampFromDate(date)
        result shouldBeEqualTo 1611950162000
    }

    @Test
    fun `Check is the same day` () {
        val resultOne = DateUtils.isTheSameDay(1612193618000, 1611950162000)
        resultOne shouldBeEqualTo false
        val resultTwo = DateUtils.isTheSameDay(1612193618000, 1612193618000)
        resultTwo shouldBeEqualTo true
        val resultThree = DateUtils.isTheSameDay(1605571200000, 1605653999000)
        resultThree shouldBeEqualTo true
        val resultFour = DateUtils.isTheSameDay(1605571200000, 1605657600000)
        resultFour shouldBeEqualTo false
    }
}