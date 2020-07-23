package minerva.android.onboarding.base


import android.content.Context
import androidx.fragment.app.Fragment

import minerva.android.onboarding.OnBoardingFragmentListener

abstract class BaseOnBoardingFragment : Fragment() {

    internal lateinit var listener: OnBoardingFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnBoardingFragmentListener
    }
}
