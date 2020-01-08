package minerva.android.identities

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
import minerva.android.identities.data.IDENTITY_DATA_LIST
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Identity
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.*

class EditIdentityFragment : Fragment() {

    private var index: Int = Int.InvalidIndex
    private val viewModel: EditIdentityViewModel by viewModel()

    private var viewGroup: ViewGroup? = null

    private lateinit var identity: Identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewGroup = container
        return inflater.inflate(R.layout.fragment_edit_identity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        identityName.setText(identity.name)
        name.setText(identity.data[IDENTITY_DATA_LIST[0]])
        email.setText(identity.data[IDENTITY_DATA_LIST[1]])
        phoneNumber.setText(identity.data[IDENTITY_DATA_LIST[2]])
        birthDate.setText(identity.data[IDENTITY_DATA_LIST[3]])
        addressLine1.setText(identity.data[IDENTITY_DATA_LIST[4]])
        addressLine2.setText(identity.data[IDENTITY_DATA_LIST[5]])
        city.setText(identity.data[IDENTITY_DATA_LIST[6]])
        postcode.setText(identity.data[IDENTITY_DATA_LIST[7]])
        country.setText(identity.data[IDENTITY_DATA_LIST[8]])

        confirmButton.text = if (index == Int.InvalidIndex) getString(R.string.create_new_identity)
        else getString(R.string.update_identity)
    }

    private fun saveIdentity() {
        val newIdentity = Identity(
            identity.index,
            identityName.text.toString(),
            data = prepareFormData()
        )
        viewModel.saveIdentity(newIdentity)
    }

    //TODO Will be changed in phase 2 for dynamic labels
    private fun prepareFormData(): LinkedHashMap<String, String> {
        val map = mutableMapOf<String, String>()
        name.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[0]] = this
        }
        email.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[1]] = this
        }
        phoneNumber.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[2]] = this
        }
        birthDate.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[3]] = this
        }
        addressLine1.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[4]] = this
        }
        addressLine2.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[5]] = this
        }
        city.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[6]] = this
        }
        postcode.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[7]] = this
        }
        country.text.toString().apply {
            if (isNotEmpty()) map[IDENTITY_DATA_LIST[8]] = this
        }
        return map as LinkedHashMap<String, String>
    }

    private fun initializeFragment() {
        viewModel.editIdentityLiveData.observe(this, EventObserver { initializeView(it) })
        viewModel.saveCompletedLiveData.observe(this, EventObserver { activity?.finish() })
        viewModel.saveErrorLiveData.observe(this, EventObserver { showErrorMessage(it.message) })
        arguments?.let {
            index = it.getInt(INDEX)
            viewModel.loadIdentity(index, getString(R.string.identity))
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

            val datePickerDialog = DatePickerDialog(it, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                birthDate.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, year))
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
