package minerva.android.onboarding.restore


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_restore_wallet.*
import minerva.android.R
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.onboarding.base.BaseOnBoardingFragment
import minerva.android.wrapper.TextWatcherWrapper
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreWalletFragment : BaseOnBoardingFragment() {

    private val viewModel: RestoreWalletViewModel by viewModel()
    private lateinit var mnemonic: String

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

    override fun onResume() {
        super.onResume()
        prepareObservers()
        prepareMnemonicLengthValidator()
    }

    private fun prepareObservers() {
        viewModel.validateMnemonicLiveData.observe(this, Observer { handleMnemonicValidation(it) })
    }

    private fun handleMnemonicValidation(invalidMnemonicWords: List<String>) {
        if (invalidMnemonicWords.isEmpty()) {
            errorMessage.invisible()
            listener.showMainActivity()
        } else {
            restoreWalletButton.isEnabled = false
            errorMessage.visible()
            errorMessage.text = "${getString(R.string.check_incorrect_mnemonic_words)} $invalidMnemonicWords"
        }
    }

    private fun prepareMnemonicLengthValidator() {
        mnemonicEditText.addTextChangedListener(object : TextWatcherWrapper() {
            override fun onTextChanged(content: CharSequence?) {
                handleMnemonicLengthValidation(content)
            }
        })
    }

    private fun handleMnemonicLengthValidation(content: CharSequence?) {
        restoreWalletButton.isEnabled = if (viewModel.isMnemonicLengthValid(content)) {
            mnemonic = content.toString()
            true
        } else {
            errorMessage.invisible()
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