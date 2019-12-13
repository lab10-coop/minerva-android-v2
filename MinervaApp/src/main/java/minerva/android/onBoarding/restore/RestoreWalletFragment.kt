package minerva.android.onBoarding.restore


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_restore_wallet.*
import minerva.android.R
import minerva.android.onBoarding.base.BaseOnBoardingFragment


class RestoreWalletFragment : BaseOnBoardingFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_restore_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleRestoreWalletButton()
    }

    private fun handleRestoreWalletButton() {
        restoreWalletButton.setOnClickListener {
            //            TODO add restoring wallet mechanism
            listener.showMainActivity()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RestoreWalletFragment()

        val TAG: String = this::class.java.canonicalName ?: "RestoreWalletFragment"
    }
}