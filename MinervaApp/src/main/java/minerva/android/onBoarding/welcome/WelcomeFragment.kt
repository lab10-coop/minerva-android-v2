package minerva.android.onBoarding.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_welcome.*
import minerva.android.R
import minerva.android.onBoarding.base.BaseOnBoardingFragment


class WelcomeFragment : BaseOnBoardingFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleCreateWalletButton()
        handleRestoreWalletButton()
    }

    private fun handleCreateWalletButton() {
        createWalletButton.setOnClickListener {
            listener.showCreateWalletFragment()
        }
    }

    private fun handleRestoreWalletButton() {
        restoreWalletButton.setOnClickListener {
            listener.showRestoreWalletFragment()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeFragment()

        val TAG: String = this::class.java.canonicalName ?: "WelcomeFragment"
    }
}
