package minerva.android.onboarding.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnBoardingViewData(
    @StringRes val stringId: Int,
    @DrawableRes val drawableId: Int
)