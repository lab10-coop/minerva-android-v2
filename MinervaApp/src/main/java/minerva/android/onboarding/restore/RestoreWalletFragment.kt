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
import minerva.android.onboarding.base.BaseOnBoardingFragment
import minerva.android.onboarding.restore.dialog.ImportWalletWithoutBackupDialog
import minerva.android.onboarding.restore.state.*
import minerva.android.utils.AlertDialogHandler
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreWalletFragment : BaseOnBoardingFragment(R.layout.fragment_restore_wallet) {

    private val viewModel: RestoreWalletViewModel by viewModel()
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

    private fun handleRestoreWalletButton() {
        binding.restoreWalletButton.setOnClickListener {
            viewModel.restoreWallet()
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            restoreWalletSuccess.observe(this@RestoreWalletFragment, Observer { listener.showMainActivity() })
            restoreWalletState.observe(this@RestoreWalletFragment, Observer { state ->
                when (state) {
                    is ValidMnemonic -> enableImportButton(true)
                    is InvalidMnemonicLength -> enableImportButton(false)
                    is InvalidMnemonicWords -> handleInvalidMnemonic()
                    is WalletConfigNotFound ->
                        ImportWalletWithoutBackupDialog(requireContext()) { viewModel.createWalletConfig() }.show()
                    is Loading -> if (state.isLoading) showLoader() else hideLoader()
                    is GenericServerError ->
                        AlertDialogHandler.showDialog(
                            requireContext(),
                            getString(R.string.error_title),
                            getString(R.string.backup_server_error)
                        )
                    is WalletConfigCreated -> listener.showMainActivity()
                }
            })
        }
    }

    private fun enableImportButton(isEnabled: Boolean) = with(binding) {
        errorMessage.invisible()
        restoreWalletButton.isEnabled = isEnabled
    }

    private fun hideLoader() = with(binding) {
        restoreWalletButton.visible()
        restoreWalletProgressBar.gone()
    }

    private fun showLoader() = with(binding) {
        restoreWalletButton.invisible()
        restoreWalletProgressBar.visible()
    }

    @SuppressLint("SetTextI18n")
    private fun handleInvalidMnemonic() = with(binding) {
        restoreWalletButton.isEnabled = false
        errorMessage.visible()
        errorMessage.text = getString(R.string.unknown_words)
    }

    private fun prepareMnemonicLengthValidator() {
        binding.mnemonicEditText.addTextChangedListener(object : TextWatcherWrapper() {
            override fun onTextChanged(s: CharSequence?) {
                viewModel.validateMnemonic(s)
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = RestoreWalletFragment()
    }
}