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
import minerva.android.identities.adapter.IdentityFragmentListener
import minerva.android.identities.data.getIdentityDataLabel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.DateUtils.DATE_FORMAT
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.widget.clubCard.ClubCard

class IdentityDataContent(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val description = TextView(context)
    private val views: MutableList<View> = arrayListOf()
    var isOpen: Boolean = false
    private lateinit var listener: IdentityFragmentListener

    init {
        orientation = VERTICAL
    }

    fun prepareDataContainerFields(identity: Identity, credentials: List<Credential>) {
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

    fun setListener(listener: IdentityFragmentListener) {
        this.listener = listener
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
        credentials.forEach { credential ->
            IdentityBindedItem(context).let {
                it.setIconUrl(credential.iconUrl)
                it.setDateAndName(credential.name, DateUtils.getDateFromTimestamp(credential.lastUsed, DATE_FORMAT))
                it.popup_menu.setOnClickListener { item -> showMenu(item.popup_menu, credential) }
                it.setOnItemClickListener { ClubCard(context, credential).show() }
                views.add(it)
                addView(it)
            }
        }
    }

    private fun prepareServices(services: List<Service>) {
        if (services.isNotEmpty()) addHeader(R.string.connected_services)
        services.forEach { service ->
            val bindedService = IdentityBindedItem(context)
            bindedService.setDateAndName(service.name, DateUtils.getDateFromTimestamp(service.lastUsed, DATE_FORMAT))
            //TODO change to adding proper icon based on the service type
            bindedService.setIcon(R.drawable.ic_backup)
            bindedService.popup_menu.setOnClickListener { showMenu(bindedService.popup_menu, service) }
            views.add(bindedService)
            addView(bindedService)
        }
    }

    private fun showMenu(anchor: View, minervaPrimitive: MinervaPrimitive) {
        PopupMenu(context, anchor).apply {
            menuInflater.inflate(R.menu.remove_menu, menu)
            menu.findItem(R.id.disconnect).isVisible = false
            gravity = Gravity.END
            setOnMenuItemClickListener {
                if (it.itemId == R.id.remove) listener.onBindedItemDeleted(minervaPrimitive)
                true
            }
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