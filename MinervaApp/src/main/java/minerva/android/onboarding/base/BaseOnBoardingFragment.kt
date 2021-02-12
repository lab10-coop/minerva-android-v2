package minerva.android.onboarding.base


import android.content.Context
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

import minerva.android.onboarding.OnBoardingFragmentListener

abstract class BaseOnBoardingFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    internal lateinit var listener: OnBoardingFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnBoardingFragmentListener
    }
}
