package minerva.android.identities.edit

import android.app.DatePickerDialog
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_edit_identity.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.defs.IdentityField.Companion.ADDRESS_1
import minerva.android.walletmanager.model.defs.IdentityField.Companion.ADDRESS_2
import minerva.android.walletmanager.model.defs.IdentityField.Companion.BIRTH_DATE
import minerva.android.walletmanager.model.defs.IdentityField.Companion.CITY
import minerva.android.walletmanager.model.defs.IdentityField.Companion.COUNTRY
import minerva.android.walletmanager.model.defs.IdentityField.Companion.EMAIL
import minerva.android.walletmanager.model.defs.IdentityField.Companion.NAME
import minerva.android.walletmanager.model.defs.IdentityField.Companion.PHONE_NUMBER
import minerva.android.walletmanager.model.defs.IdentityField.Companion.POSTCODE
import minerva.android.walletmanager.model.defs.WalletActionStatus
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.*

class EditIdentityFragment : Fragment() {

    private var index: Int = Int.InvalidIndex
    private val viewModel: EditIdentityViewModel by viewModel()

    private var viewGroup: ViewGroup? = null

    private lateinit var identity: Identity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewGroup = container
        return inflater.inflate(R.layout.fragment_edit_identity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFragment()
        moreFields.setOnClickListener {
            TransitionManager.beginDelayedTransition(viewGroup)
            moreFields.gone()
            addressHeader.visible()
            addressLine1Layout.visible()
            addressLine2Layout.visible()
            cityLayout.visible()
            postcodeLayout.visible()
            countryLayout.visible()
        }
        identityName.afterTextChanged { profileLogo.createLogo(it) }
        identityName.onFocusLost { identityName.error = isEmpty(it) }
        email.onFocusLost { email.error = getEmailErrorMessage(it) }
        birthDate.setOnClickListener { showDialogPicker() }
        confirmButton.setOnClickListener { onConfirmButtonClicked() }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    private fun onConfirmButtonClicked() {
        email.text.toString().apply {
            if (isNotEmailAndIsNotEmpty(this)) {
                showErrorMessage(getString(R.string.wrong_email))
                return
            }
        }
        if (identityName.text.toString().isEmpty()) {
            showErrorMessage(getString(R.string.empty_identity_name))
            return
        }
        saveIdentity()
    }

    private fun getEmailErrorMessage(text: String): String? =
        if (text.isEmail()) null
        else getString(R.string.wrong_email)

    private fun isEmpty(text: String): String? =
        if (text.isEmpty()) getString(R.string.can_not_be_empty)
        else null

    private fun isNotEmailAndIsNotEmpty(text: String): Boolean = !text.isEmail() && text.isNotEmpty()

    private fun initializeView(identity: Identity) {
        this.identity = identity
        setIdentityData(identity)
        confirmButton.text = if (index == Int.InvalidIndex) getString(R.string.create_new_identity)
        else getString(R.string.update_identity)
    }

    private fun setIdentityData(identity: Identity) {
        identityName.setText(identity.name)
        accountName.setText(identity.data[NAME])
        email.setText(identity.data[EMAIL])
        phoneNumber.setText(identity.data[PHONE_NUMBER])
        birthDate.setText(identity.data[BIRTH_DATE])
        addressLine1.setText(identity.data[ADDRESS_1])
        addressLine2.setText(identity.data[ADDRESS_2])
        city.setText(identity.data[CITY])
        postcode.setText(identity.data[POSTCODE])
        country.setText(identity.data[COUNTRY])
    }

    private fun saveIdentity() {
        val newIdentity = Identity(
            identity.index,
            identityName.text.toString(),
            data = prepareFormData()
        )
        viewModel.saveIdentity(newIdentity, getActionStatus())
    }

    //TODO change for dynamic label generation
    private fun prepareFormData(): LinkedHashMap<String, String> {
        val map = mutableMapOf<String, String>()
        accountName.text.toString().apply {
            if (isNotEmpty()) map[NAME] = this
        }
        email.text.toString().apply {
            if (isNotEmpty()) map[EMAIL] = this
        }
        phoneNumber.text.toString().apply {
            if (isNotEmpty()) map[PHONE_NUMBER] = this
        }
        birthDate.text.toString().apply {
            if (isNotEmpty()) map[BIRTH_DATE] = this
        }
        addressLine1.text.toString().apply {
            if (isNotEmpty()) map[ADDRESS_1] = this
        }
        addressLine2.text.toString().apply {
            if (isNotEmpty()) map[ADDRESS_2] = this
        }
        city.text.toString().apply {
            if (isNotEmpty()) map[CITY] = this
        }
        postcode.text.toString().apply {
            if (isNotEmpty()) map[POSTCODE] = this
        }
        country.text.toString().apply {
            if (isNotEmpty()) map[COUNTRY] = this
        }
        return map as LinkedHashMap<String, String>
    }

    private fun initializeFragment() {
        viewModel.apply {
            editIdentityLiveData.observe(viewLifecycleOwner, EventObserver { initializeView(it) })
            saveCompletedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.onBackPressed() })
            saveErrorLiveData.observe(viewLifecycleOwner, EventObserver { showErrorMessage(it.message) })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
        }
        arguments?.let {
            index = it.getInt(INDEX)
            viewModel.loadIdentity(index, getString(R.string.identity))
        }
    }

    private fun getActionStatus(): Int =
        if (index == Int.InvalidIndex) WalletActionStatus.ADDED
        else WalletActionStatus.CHANGED

    private fun handleLoader(isLoading: Boolean) {
        if (isLoading) {
            saveIdentityProgressBar.visible()
            confirmButton.gone()
        } else {
            saveIdentityProgressBar.gone()
            confirmButton.visible()
        }
    }

    private fun showErrorMessage(message: String?) {
        Timber.e(message ?: getString(R.string.unexpected_error))
        Toast.makeText(this.context, message ?: getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
    }

    private fun showDialogPicker() {
        context?.let {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(it, DatePickerDialog.OnDateSetListener { _, birthYear, monthOfYear, dayOfMonth ->
                birthDate.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, birthYear))
            }, year, month, day)
            datePickerDialog.apply {
                datePicker.maxDate = System.currentTimeMillis()
                //this line is responsible for showing years in date picker first
                datePicker.touchables[0].performClick()
                show()
            }
        }
    }

    companion object {
        private const val INDEX = "index"
        private const val DATE_FORMAT = "%d.%d.%d"

        @JvmStatic
        fun newInstance(index: Int) =
            EditIdentityFragment().apply {
                arguments = Bundle().apply {
                    putInt(INDEX, index)
                }
            }
    }
}
