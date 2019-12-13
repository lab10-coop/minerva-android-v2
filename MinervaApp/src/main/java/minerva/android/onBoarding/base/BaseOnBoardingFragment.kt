package minerva.android.onBoarding.base


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import minerva.android.R
import minerva.android.onBoarding.OnBoardingFragmentListener

abstract class BaseOnBoardingFragment : Fragment() {

    internal lateinit var listener: OnBoardingFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnBoardingFragmentListener
    }
}
