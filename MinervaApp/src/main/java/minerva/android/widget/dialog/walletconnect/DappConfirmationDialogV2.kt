package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import minerva.android.R
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.accounts.walletconnect.DappAddressSpinnerAdapter
import minerva.android.accounts.walletconnect.WalletConnectV2AlertType
import minerva.android.databinding.DappConfirmationDialogV2Binding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.utils.AddressConverter
import minerva.android.walletmanager.utils.AddressType
import minerva.android.widget.DynamicWidthSpinner
import minerva.android.widget.dialog.models.ViewDetailsV2

class DappConfirmationDialogV2(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappConfirmationDialogV2Binding = DappConfirmationDialogV2Binding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)
    //current DApp session wallet connection
    var dAppSessionMeta: WalletConnectPeerMeta? = null
    var numberOfProvidedNetworks = INITIAL_PROVIDED_NETWORKS

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
        binding.confirmationView.hideRequestedData()
    }

    /**
     * Set View - prepare global variables and set some state for popup dialog
     * @param meta - set current wallet connection DApp session (from db)
     * @param viewDetails - popup dialog view details
     */
    fun setView(
        meta: WalletConnectPeerMeta,
        viewDetails: ViewDetailsV2,
        _numberOfProvidedNetworks: Int
    )
    = with(binding) {
        //set current wallet connection dapp session
        dAppSessionMeta = meta
        setupHeader(
            meta.name,
            context.getString(R.string.requested_networks, viewDetails.networkNames.joinToString(
                ACCOUNT_DELIMITER)),
            getIcon(meta)
        )
        binding.apply {
            confirmationButtons.confirm.text = viewDetails.confirmButtonName
            connectionName.text = viewDetails.connectionName
        }
        numberOfProvidedNetworks = _numberOfProvidedNetworks
    }

    //temporary realization
    fun setupAddressSpinnerV2(availableAddresses: List<AddressWrapper>, onAddressSelected: (String) -> Unit) {
        networkHeader.apply {
            accountSpinner.visibility = View.GONE//! hide old code realization
            addressSpinner.visibility = View.GONE//! hide old code realization

            if (isAddressSpinnerVisible(availableAddresses.size)) {
                //create list with appropriate address appearance (index + short address)
                val listOfAddresses: List<String> = availableAddresses.map { addressWrapper ->
                    "#${addressWrapper.index.inc()}: ${AddressConverter.getShortAddress(AddressType.NORMAL_ADDRESS, addressWrapper.address)}"
                }
                val adapter = ArrayAdapter(context, R.layout.dropdown_list_item, listOfAddresses)//create adapter and filling it with help of addresses
                dropdownMenuContainer.visibility = View.VISIBLE//show dropdown container
                dropdownMenuItemsContainer.apply {
                    var bgChanged: Boolean = false//for prevent multiple updating menu of background state
                    setAdapter(adapter)
                    setOnClickListener {//set custom background appearance
                        if (!bgChanged) {
                            dropdownMenuItemWrapper.setBoxCornerRadii(24F, 24F, 0F, 0F)
                            bgChanged = true
                        }
                        setOnItemClickListener { _, _, position, _ ->//paste selected address to callback
                            onAddressSelected(availableAddresses[position].address)
                        }
                    }
                }
            } else {
                dropdownMenuContainer.visibility = View.GONE
            }
        }
    }

    fun setupAddressSpinner(availableAddresses: List<AddressWrapper>, onAddressSelected: (String) -> Unit) {
        networkHeader.accountSpinner.apply {
            val accountAdapter = DappAddressSpinnerAdapter(
                context,
                R.layout.spinner_network_wallet_connect,
                availableAddresses
            ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }

            visibleOrGone(isAddressSpinnerVisible(availableAddresses.size))
            addOnGlobalLayoutListener {
                accountAdapter.selectedItemWidth = networkHeader.accountSpinner.width
            }
            adapter = accountAdapter
            val defaultPosition = Int.FirstIndex
            prepareSpinner(R.drawable.rounded_background_purple_frame, defaultPosition) { position, view ->
                onAddressSelected(accountAdapter.getItem(position).address)
                accountAdapter.selectedItemWidth = view?.width
            }
        }
    }

    private fun isAddressSpinnerVisible(listSize: Int): Boolean = listSize > Int.EmptyResource && networkHeader.addAccount.isGone

    private fun DynamicWidthSpinner.prepareSpinner(backgroundResId: Int, selectionIndex: Int, onClick: (Int, View?) -> Unit) {
        setBackgroundResource(backgroundResId)
        setPopupBackgroundResource(R.drawable.rounded_small_white_background)
        setSelection(selectionIndex, false)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onClick(position, view)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    // todo: check if this is correct
    private fun setNoAlert() = with(binding) {
        warringIcon.gone()
        warning.gone()
        manual.visible()
        confirmationButtons.confirm.isEnabled = true
        networkHeader.apply {
            networkWarning.visible()
            networkWarning.text = context.getString(R.string.fully_supported, numberOfProvidedNetworks)
            addAccount.gone()
            accountSpinner.gone()
            networkSpinner.gone()
            addressSpinner.visible()
        }
    }

    private fun setUnsupportedNetworkWarning() = with(binding) {
        networkHeader.apply {
            networkWarning.visible()
            networkWarning.text = context.getString(R.string.request_not_supported)
            addAccount.gone()
            accountSpinner.gone()
            networkSpinner.gone()
            addressSpinner.visible()
        }
        confirmationButtons.confirm.isEnabled = false
        manual.text = context.getString(R.string.website_networks_not_supported)
    }

    private fun setOtherUnsupportedWarning() = with(binding) {
        networkHeader.apply {
            networkWarning.visible()
            networkWarning.text = context.getString(R.string.fully_supported, numberOfProvidedNetworks)
            addAccount.gone()
            accountSpinner.gone()
            networkSpinner.gone()
            addressSpinner.visible()
        }
        confirmationButtons.confirm.isEnabled = false
        manual.text = context.getString(R.string.events_methods_not_supported)
    }

    fun setWarnings(alertType: WalletConnectV2AlertType) {
        when (alertType) {
            WalletConnectV2AlertType.NO_ALERT -> setNoAlert()
            WalletConnectV2AlertType.UNSUPPORTED_NETWORK_WARNING -> setUnsupportedNetworkWarning()
            WalletConnectV2AlertType.OTHER_UNSUPPORTED -> setOtherUnsupportedWarning()
        }
    }

    private fun DappConfirmationDialogV2Binding.getIcon(meta: WalletConnectPeerMeta): Any =
        if (meta.icons.isEmpty()) {
            confirmationView.setDefaultIcon()
            R.drawable.ic_services
        } else {
            confirmationView.setIcon(meta.icons[FIRST_ICON])
            meta.icons[FIRST_ICON]
        }

    companion object {
        const val INITIAL_PROVIDED_NETWORKS = 0
        const val ACCOUNT_DELIMITER = " • "
    }
}