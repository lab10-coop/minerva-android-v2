package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import kotlinx.android.synthetic.main.identity_binded_item.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.identities.data.getIdentityDataLabel
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.defs.VerifiableCredentialType

class IdentityDataContent(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val description = TextView(context)
    private val views: MutableList<View> = arrayListOf()
    var isOpen: Boolean = false

    init {
        orientation = VERTICAL
    }

    fun prepareDataContainerFields(identity: Identity) {
        views.clear()
        removeAllViews()
        with(identity) {
            preparePersonalData(personalData)
            prepareCredentials(credentials)
            prepareServices(services)
            prepareDescription(personalData)
        }
        close()
    }

    private fun preparePersonalData(personalData: LinkedHashMap<String, String>) {
        personalData.forEach { (key, value) ->
            val titledTextView = TitledTextView(context)
            titledTextView.setTitleAndBody(getIdentityDataLabel(context, key), value)
            setDefaultTopPadding(titledTextView)
            views.add(titledTextView)
            addView(titledTextView)
        }
    }

    private fun prepareCredentials(credentials: List<Credential>) {
        if (credentials.isNotEmpty()) addHeader(R.string.credentials)
        credentials.forEach {
            val bindedCredential = IdentityBindedItem(context)
            bindedCredential.setDateAndName(it.name, it.lastUsed)
            setCredentialIcon(it, bindedCredential)
            bindedCredential.popupMenu.setOnClickListener { showMenu(bindedCredential.popupMenu) }
            views.add(bindedCredential)
            addView(bindedCredential)
        }
    }

    private fun setCredentialIcon(it: Credential, bindedCredential: IdentityBindedItem) {
        when {
            it.issuer == CredentialType.OAMTC && it.type == VerifiableCredentialType.AUTOMOTIVE_CLUB -> bindedCredential.setIcon(R.drawable.ic_oamtc_credential)
            else -> bindedCredential.setIcon(R.drawable.ic_minerva_icon)
        }
    }

    private fun prepareServices(services: List<Service>) {
        if (services.isNotEmpty()) addHeader(R.string.connected_services)
        services.forEach {
            val bindedService = IdentityBindedItem(context)
            bindedService.setDateAndName(it.name, it.lastUsed)
            //TODO change to adding proper icon based on the service type
            bindedService.setIcon(R.drawable.ic_backup_icon)
            bindedService.popupMenu.setOnClickListener { showMenu(bindedService.popupMenu) }
            views.add(bindedService)
            addView(bindedService)
        }
    }

    private fun showMenu(anchor: View) {
        PopupMenu(context, anchor).apply {
            menuInflater.inflate(R.menu.remove_menu, menu)
            gravity = Gravity.END
            show()
        }
    }

    private fun addHeader(id: Int) {
        val header = TextView(context)
        header.setTextAppearance(R.style.BindedHeaderStyle)
        header.text = context.getString(id)
        setDefaultTopPadding(header)
        views.add(header)
        addView(header)
    }

    fun open() {
        showEverything()
        if (views.isNotEmpty()) description.gone()
        isOpen = true
    }

    fun close() {
        if (views.size <= FIELD_DESCRIPTION_LIMIT) showEverything()
        else showOnlyFirstElement()
        description.visible()
        isOpen = false
    }

    private fun showEverything() = views.forEach { it.visible() }

    private fun showOnlyFirstElement() {
        views.forEach { view -> view.gone() }
    }

    private fun prepareDescription(map: LinkedHashMap<String, String>) {
        description.text = when {
            map.size > FIELD_DESCRIPTION_LIMIT -> createDescriptionText(map.keys)
            else -> return
        }
        if (map.isEmpty()) description.gone()
        setDefaultTopPadding(description)
        addView(description)
    }

    private fun createDescriptionText(keys: MutableSet<String>): String {
        with(StringBuilder(PLUS_SIGN)) {
            keys.forEachIndexed { index, key ->
                append(getIdentityDataLabel(context, key))
                if (index < keys.size - 1) append(COMMA_SIGN)
            }
            return this.toString()
        }
    }

    private fun setDefaultTopPadding(view: View) {
        view.setPadding(NO_PADDING, resources.getDimensionPixelOffset(R.dimen.margin_normal), NO_PADDING, NO_PADDING)
    }

    companion object {
        private const val NO_PADDING = 0
        const val FIELD_DESCRIPTION_LIMIT = 1
        private const val PLUS_SIGN = "+ "
        private const val COMMA_SIGN = ", "
    }
}