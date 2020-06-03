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

fun generateColor(value: String, opacity: Boolean = false): Int {
    abs(value.hashCode() % color.size).apply {
        return if (opacity) opacityColor[this]
        else color[this]
    }
}