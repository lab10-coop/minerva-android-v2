package minerva.android.widget

import minerva.android.R
import kotlin.math.abs

fun generateColor(value: String, opacity: Boolean = false): Int {
    val index = abs(value.hashCode() % color.size)
    return if (opacity) opacityColor[index]
    else color[index]
}

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
