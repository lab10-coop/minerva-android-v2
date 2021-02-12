package minerva.android.onboarding.restore

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.databinding.FragmentRestoreWalletBinding
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.wrapper.TextWatcherWrapper
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreWalletFragment : BaseOnBoardingFragment(R.layout.fragment_restore_wallet) {

    private val viewModel: RestoreWalletViewModel by viewModel()
    private lateinit var mnemonic: String
    private lateinit var binding: FragmentRestoreWalletBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRestoreWalletBinding.bind(view)
        handleRestoreWalletButton()
    }

    override fun onResume() {
        super.onResume()
        prepareObservers()
        prepareMnemonicLengthValidator()
    }

    private fun prepareObservers() {
        viewModel.apply {
            invalidMnemonicLiveData.observe(this@RestoreWalletFragment, EventObserver { handleInvalidMnemonic(it) })
            restoreWalletLiveData.observe(this@RestoreWalletFragment, Observer { listener.showMainActivity() })
            loadingLiveData.observe(this@RestoreWalletFragment, EventObserver { if (it) showLoader() else hideLoader() })
            walletConfigNotFoundLiveData.observe(
                this@RestoreWalletFragment,
                EventObserver { handleError(R.string.no_such_file_error_message) })
        }
    }

    private fun hideLoader() = with(binding) {
        restoreWalletButton.visible()
        restoreWalletProgressBar.gone()
    }

    private fun showLoader() = with(binding) {
        restoreWalletButton.invisible()
        restoreWalletProgressBar.visible()
    }

    private fun handleError(messageId: Int) = with(binding) {
        errorMessage.visible()
        errorMessage.text = getString(messageId)
    }

    @SuppressLint("SetTextI18n")
    private fun handleInvalidMnemonic(invalidMnemonicWords: List<String>) = with(binding) {
        restoreWalletButton.isEnabled = false
        errorMessage.visible()
        errorMessage.text = "${getString(R.string.check_incorrect_mnemonic_words)} $invalidMnemonicWords"
    }

    private fun prepareMnemonicLengthValidator() {
        binding.mnemonicEditText.addTextChangedListener(object : TextWatcherWrapper() {
            override fun onTextChanged(s: CharSequence?) {
                handleMnemonicLengthValidation(s)
            }
        })
    }

    private fun handleMnemonicLengthValidation(content: CharSequence?) = with(binding) {
        errorMessage.invisible()
        restoreWalletButton.isEnabled = if (viewModel.isMnemonicLengthValid(content)) {
            mnemonic = content.toString()
            true
        } else {
            false
        }
    }

    private fun handleRestoreWalletButton() {
        binding.restoreWalletButton.setOnClickListener {
            viewModel.validateMnemonic(mnemonic)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RestoreWalletFragment()
    }
}