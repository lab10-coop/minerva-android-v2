package minerva.android.identities.edit

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import minerva.android.R
import minerva.android.databinding.FragmentEditIdentityBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.ServiceQrCode
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
import minerva.android.walletmanager.utils.AddressConverter
import minerva.android.walletmanager.utils.AddressType
import minerva.android.widget.LetterLogo
import minerva.android.widget.ProfileImage
import minerva.android.widget.dialog.ProfileImageDialog
import minerva.android.widget.dialog.ProfileImageDialog.Companion.TAKE_PHOTO_REQUEST
import minerva.android.widget.dialog.ConfirmDataDialog
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import minerva.android.wrapped.WrappedActivity.Companion.POSITION
import minerva.android.wrapped.WrappedActivity.Companion.SERVICE_QR_CODE
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.*

class EditIdentityFragment : BaseFragment(R.layout.fragment_edit_identity) {
    private var index: Int = Int.InvalidIndex
    private var serviceQrCode: ServiceQrCode? = null
    private lateinit var identity: Identity
    private lateinit var profileImageDialog: ProfileImageDialog
    private val viewModel: EditIdentityViewModel by viewModel()
    private var wasCustomPhotoSet: Boolean = false
    private lateinit var binding: FragmentEditIdentityBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditIdentityBinding.bind(view)
        initializeFragment()
        binding.apply {
            moreFields.setOnClickListener {
                TransitionManager.beginDelayedTransition(binding.root)
                moreFields.gone()
                addressHeader.visible()
                addressLineOneLayout.visible()
                addressLineTwoLayout.visible()
                cityLayout.visible()
                postcodeLayout.visible()
                countryLayout.visible()
            }
            birthDate.setOnClickListener { showDialogPicker() }
            confirmButton.setOnClickListener { onConfirmButtonClicked() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    private fun prepareImageUri(requestCode: Int, data: Intent?): Uri? =
        if (requestCode == TAKE_PHOTO_REQUEST) data?.data
        else profileImageDialog.imageUri

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        profileImageDialog.onRequestPermissionsResult(requestCode, permissions)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Glide.with(requireContext())
                .load(prepareImageUri(requestCode, data))
                .apply(RequestOptions.circleCropTransform()).into(binding.profileImage)
            wasCustomPhotoSet = true
        }
    }

    private fun showConfirmDialog() {
        ConfirmDataDialog.Builder(requireContext())
            .title(getString(R.string.provide_following_data))
            .data(prepareRequestedDataMap())
            .positiveAction { saveIdentity() }
            .show()
    }

    private fun prepareRequestedDataMap(): Map<String, String> =
        mutableMapOf<String, String>().apply {
            this[DID] = AddressConverter.getShortAddress(AddressType.DID_ADDRESS, identity.did)
            prepareFormData().let { currentData ->
                serviceQrCode?.requestedData?.let {
                    it.forEach { key ->
                        this[key.capitalize()] = currentData[key] ?: String.Empty
                    }
                }
            }
        }

    private fun isConfirmDialogRequested(): Boolean = serviceQrCode?.requestedData != null

    private fun onConfirmButtonClicked() {
        if (!checkRequestedData()) {
            showErrorMessage(getString(R.string.missing_required_data))
            return
        }
        binding.apply {
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
        }
        if (isConfirmDialogRequested()) showConfirmDialog()
        else saveIdentity()
    }

    private fun getEmailErrorMessage(text: String): String? =
        if (text.isEmail()) null
        else getString(R.string.wrong_email)

    private fun isBlank(text: String): String? =
        if (text.isBlank()) getString(R.string.can_not_be_empty)
        else null

    private fun isNotEmailAndIsNotEmpty(text: String): Boolean = !text.isEmail() && text.isNotEmpty()

    private fun initializeView(identity: Identity) {
        this.identity = Identity(identity)
        setIdentityData(this.identity)
        binding.confirmButton.text = if (index == Int.InvalidIndex) getString(R.string.create_new_identity)
        else getString(R.string.update_identity)
    }

    private fun setIdentityData(identity: Identity) {
        with(identity) {
            binding.apply {
                identityName.setText(name)
                accountName.setText(personalData[NAME])
                this@EditIdentityFragment.binding.did.apply {
                    setTitleAndBody(getString(R.string.did), this@with.did)
                    setSingleLine()
                    makeEnabled(false)
                }
                email.setText(personalData[EMAIL])
                phoneNumber.setText(personalData[PHONE_NUMBER])
                birthDate.setText(personalData[BIRTH_DATE])
                addressLineOne.setText(personalData[ADDRESS_1])
                addressLineTwo.setText(personalData[ADDRESS_2])
                city.setText(personalData[CITY])
                postcode.setText(personalData[POSTCODE])
                country.setText(personalData[COUNTRY])
                ProfileImage.load(profileImage, identity)
            }
        }
    }

    private fun resetPhoto() {
        binding.apply {
            profileImage.setImageDrawable(LetterLogo.createLogo(requireContext(), identityName.text.toString()))
        }
        wasCustomPhotoSet = false
        identity.profileImageBitmap = null
    }

    private fun saveIdentity() {
        viewModel.saveIdentity(getUpdatedIdentity(), getActionStatus())
    }

    private fun getUpdatedIdentity(): Identity {
        binding.apply {
            (if (wasCustomPhotoSet) profileImage.drawable.toBitmap() else identity.profileImageBitmap).let {
                return Identity(
                    identity.index,
                    identityName.text.toString(),
                    address = identity.address,
                    personalData = prepareFormData(),
                    profileImageBitmap = it
                )
            }
        }
    }

    //TODO change for dynamic label generation
    private fun prepareFormData(): LinkedHashMap<String, String> {
        val map = mutableMapOf<String, String>()
        binding.apply {
            accountName.text.toString().trim().apply {
                if (isNotEmpty()) map[NAME] = this
            }
            email.text.toString().trim().apply {
                if (isNotEmpty()) map[EMAIL] = this
            }
            phoneNumber.text.toString().trim().apply {
                if (isNotEmpty()) map[PHONE_NUMBER] = this
            }
            birthDate.text.toString().trim().apply {
                if (isNotEmpty()) map[BIRTH_DATE] = this
            }
            addressLineOne.text.toString().trim().apply {
                if (isNotEmpty()) map[ADDRESS_1] = this
            }
            addressLineTwo.text.toString().trim().apply {
                if (isNotEmpty()) map[ADDRESS_2] = this
            }
            city.text.toString().trim().apply {
                if (isNotEmpty()) map[CITY] = this
            }
            postcode.text.toString().trim().apply {
                if (isNotEmpty()) map[POSTCODE] = this
            }
            country.text.toString().trim().apply {
                if (isNotEmpty()) map[COUNTRY] = this
            }
            return map as LinkedHashMap<String, String>
        }
    }

    private fun checkRequestedData(): Boolean {
        serviceQrCode?.requestedData?.let { requestedData ->
            prepareFormData().let { data ->
                requestedData.forEach {
                    if (data[it] == null) return false
                }
            }
        }
        return true
    }

    //TODO change for dynamic label generation
    private fun getTextInputLayout(key: String): Pair<TextInputLayout, TextInputEditText> =
        with(binding) {
            when (key) {
                NAME -> Pair(nameLayout, accountName)
                EMAIL -> Pair(emailLayout, email)
                PHONE_NUMBER -> Pair(phoneNumberLayout, phoneNumber)
                BIRTH_DATE -> Pair(birthDateLayout, birthDate)
                ADDRESS_1 -> Pair(addressLineOneLayout, addressLineOne)
                ADDRESS_2 -> Pair(addressLineTwoLayout, addressLineTwo)
                CITY -> Pair(cityLayout, city)
                POSTCODE -> Pair(postcodeLayout, postcode)
                COUNTRY -> Pair(countryLayout, country)
                else -> Pair(nameLayout, accountName)
            }
        }


    private fun initializeFragment() {
        viewModel.apply {
            editIdentityLiveData.observe(viewLifecycleOwner, EventObserver { initializeView(it) })
            saveCompletedLiveData.observe(viewLifecycleOwner, EventObserver {
                activity?.setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(INDEX, it.index)
                    putExtra(SERVICE_QR_CODE, serviceQrCode)
                })
                activity?.onBackPressed()
            })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { handleAutomaticBackupError(it, noAutomaticBackupErrorAction = { activity?.finish() }) })
        }
        arguments?.let {
            index = it.getInt(POSITION)
            serviceQrCode = it.getParcelable(SERVICE_QR_CODE)
            highlightRequestedData(serviceQrCode)
            viewModel.loadIdentity(index, getString(R.string.identity))
        }

        binding.apply {
            identityName.afterTextChanged {
                if (!wasCustomPhotoSet && identity.profileImageBitmap == null) {
                    profileImage.setImageDrawable(LetterLogo.createLogo(profileImage.context, it))
                }
            }
            checkEditTextInput(emailLayout, email) { getEmailErrorMessage(it) }
            checkEditTextInput(identityNameLayout, identityName) { isBlank(it) }

            profileImage.setOnClickListener {
                profileImageDialog = ProfileImageDialog(this@EditIdentityFragment).apply {
                    resetPhotoLiveData.observe(viewLifecycleOwner, EventObserver { resetPhoto() })
                    showDeleteOption(identity.profileImageBitmap != null)
                    show()
                }
            }
        }
    }

    private fun highlightRequestedData(serviceQrCode: ServiceQrCode?) =
        serviceQrCode?.requestedData?.let {
            it.forEach {
                getTextInputLayout(it).apply {
                    first.hint = String.format(REQUIRED_HINT_FORMAT, first.hint, getString(R.string.required))
                    first.error = String.Space
                    first.errorIconDrawable = null
                    checkEditTextInput(first, second) { isBlank(second.text.toString()) }
                }
            }
        }

    private fun checkEditTextInput(
        layout: TextInputLayout,
        editText: TextInputEditText,
        prepareErrorMessage: (String) -> String?
    ) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            layout.error = if (hasFocus) {
                if (editText.text.toString().isBlank()) editText.setText(String.Empty)
                layout.apply {
                    endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    endIconDrawable = layout.context.getDrawable(R.drawable.ic_clear)
                }
                null
            } else prepareErrorMessage(editText.text.toString())
        }
    }

    private fun getActionStatus(): Int =
        if (index == Int.InvalidIndex) WalletActionStatus.ADDED
        else WalletActionStatus.CHANGED

    private fun handleLoader(isLoading: Boolean) {
        binding.apply {
            saveIdentityProgressBar.visibleOrGone(isLoading)
            confirmButton.visibleOrGone(!isLoading)
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

            val datePickerDialog = DatePickerDialog(it, { _, birthYear, monthOfYear, dayOfMonth ->
                binding.birthDate.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, birthYear))
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
        private const val DATE_FORMAT = "%d.%d.%d"
        private const val DID = "DID"
        private const val REQUIRED_HINT_FORMAT = "%s (%s)"

        @JvmStatic
        fun newInstance(position: Int, serviceQrCode: ServiceQrCode? = null) =
            EditIdentityFragment().apply {
                arguments = Bundle().apply {
                    putInt(POSITION, position)
                    serviceQrCode?.let {
                        putParcelable(SERVICE_QR_CODE, serviceQrCode)
                    }
                }
            }
    }
}
