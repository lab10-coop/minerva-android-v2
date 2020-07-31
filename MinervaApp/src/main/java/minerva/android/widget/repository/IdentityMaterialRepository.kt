package minerva.android.widget.repository

import minerva.android.R
import kotlin.math.abs

private val color = listOf(
    R.color.colorSetOne,
    R.color.colorSetTwo,
    R.color.colorSetThree,
    R.color.colorSetFour,
    R.color.colorSetFive,
    R.color.colorSetSix,
    R.color.colorSetSeven,
    R.color.colorSetEight,
    R.color.colorSetNine,
    R.color.colorSetTen
)

private val opacityColor = listOf(
    R.color.colorSetOneOpacity,
    R.color.colorSetTwoOpacity,
    R.color.colorSetThreeOpacity,
    R.color.colorSetFourOpacity,
    R.color.colorSetFiveOpacity,
    R.color.colorSetSixOpacity,
    R.color.colorSetSevenOpacity,
    R.color.colorSetEightOpacity,
    R.color.colorSetNineOpacity,
    R.color.colorSetTenOpacity
)

private val companyColor = listOf(
    R.color.oamtc
)

private val companyOpacityColor = listOf(
    R.color.oamtcOpacity
)

private const val OAMTC_COLOR_INDEX = 0

private val oamtcKeywords = listOf(
    "ÖAMTC",
    "OeAMTC",
    "OAMTC"
)

fun generateColor(value: String, opacity: Boolean = false): Int {
    getCompanyIndexColor(value)?.let {
        return if(opacity) companyOpacityColor[it]
        else companyColor[it]
    }
    abs(value.hashCode() % color.size).let {
        return if (opacity) opacityColor[it]
        else color[it]
    }
}

private fun getCompanyIndexColor(value: String): Int? =
    if(isOamtc(value)) OAMTC_COLOR_INDEX
    else null

private fun isOamtc(value: String): Boolean {
    oamtcKeywords.forEach {
        if(value.equals(it, true)) return true
    }
    return false
}