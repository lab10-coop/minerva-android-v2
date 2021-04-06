package minerva.android.token.ramp

import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentRampBinding
import minerva.android.main.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class RampFragment : BaseFragment(R.layout.fragment_ramp) {

    private lateinit var binding: FragmentRampBinding
    private val viewModel: RampViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRampBinding.bind(view)
        initializeFragment()
    }

    private fun initializeFragment() {

    }

    companion object {
        @JvmStatic
        fun newInstance() = RampFragment()
    }
}