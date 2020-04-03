package minerva.android.onboarding.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_restore_wallet.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.wrapper.TextWatcherWrapper
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreWalletFragment : BaseOnBoardingFragment() {

    private val viewModel: RestoreWalletViewModel by viewModel()
    private lateinit var mnemonic: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_restore_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleRestoreWalletButton()
    }

    override fun onResume() {
        super.onResume()
        prepareObservers()
        prepareMnemonicLengthValidator()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun prepareObservers() {
        viewModel.apply {
            invalidMnemonicLiveData.observe(this@RestoreWalletFragment, EventObserver { handleInvalidMnemonic(it) })
            errorLiveData.observe(this@RestoreWalletFragment, EventObserver { handleError(R.string.creating_wallet_error_message) })
            restoreWalletLiveData.observe(this@RestoreWalletFragment, EventObserver { listener.showMainActivity() })
            loadingLiveData.observe(this@RestoreWalletFragment, EventObserver { if (it) showLoader() else hideLoader() })
            walletConfigNotFoundLiveData.observe(this@RestoreWalletFragment, EventObserver { handleError(R.string.no_such_file_error_message) })
        }
    }

    private fun hideLoader() {
        restoreWalletButton.visible()
        restoreWalletProgressBar.gone()
    }

    private fun showLoader() {
        restoreWalletButton.invisible()
        restoreWalletProgressBar.visible()
    }

    private fun handleError(messageId: Int) {
        errorMessage.visible()
        errorMessage.text = getString(messageId)
    }

    private fun handleInvalidMnemonic(invalidMnemonicWords: List<String>) {
        restoreWalletButton.isEnabled = false
        errorMessage.visible()
        errorMessage.text = "${getString(R.string.check_incorrect_mnemonic_words)} $invalidMnemonicWords"
    }

    private fun prepareMnemonicLengthValidator() {
        mnemonicEditText.addTextChangedListener(object : TextWatcherWrapper() {
            override fun onTextChanged(s: CharSequence?) {
                handleMnemonicLengthValidation(s)
            }
        })
    }

    private fun handleMnemonicLengthValidation(content: CharSequence?) {
        errorMessage.invisible()
        restoreWalletButton.isEnabled = if (viewModel.isMnemonicLengthValid(content)) {
            mnemonic = content.toString()
            true
        } else {
            false
        }
    }

    private fun handleRestoreWalletButton() {
        restoreWalletButton.setOnClickListener {
            viewModel.validateMnemonic(mnemonic)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RestoreWalletFragment()

        val TAG: String = this::class.java.canonicalName ?: "RestoreWalletFragment"
    }
}