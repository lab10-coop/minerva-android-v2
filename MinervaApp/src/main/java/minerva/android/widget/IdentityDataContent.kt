package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.identities.data.getIdentityDataLabel

class IdentityDataContent(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val description = TextView(context)
    private val views: MutableList<View> = arrayListOf()

    fun prepareData(map: LinkedHashMap<String, String>) {
        views.clear()
        removeAllViews()
        map.forEach { (key, value) ->
            val titledTextView = TitledTextView(context)
            titledTextView.setTitleAndBody(key, value)
            setDefaultTopPadding(titledTextView)
            views.add(titledTextView)
            addView(titledTextView)
        }
        prepareDescription(map)
        close()
    }

    fun open() {
        showEverything()
        if (views.isNotEmpty()) {
            description.gone()
        }
    }

    fun close() {
        if (views.size <= FIELD_DESCRIPTION_LIMIT) showEverything()
        else showOnlyFirstElement()
        description.visible()
    }

    private fun showEverything() = views.forEach { it.visible() }

    private fun showOnlyFirstElement() {
        views.forEachIndexed { index, view ->
            if (index > FIRST_ELEMENT) {
                view.gone()
            }
        }
    }

    private fun prepareDescription(map: LinkedHashMap<String, String>) {
        description.text = when {
            map.isEmpty() -> context.getString(R.string.no_data_available)
            map.size > FIELD_DESCRIPTION_LIMIT -> createDescriptionText(map.keys)
            else -> return
        }
        setDefaultTopPadding(description)
        addView(description)
    }

    private fun createDescriptionText(keys: MutableSet<String>): String {
        val str = StringBuilder(PLUS_SIGN)
        keys.forEachIndexed { index, key ->
            if (index > FIRST_ELEMENT) {
                str.append(getIdentityDataLabel(context, key))
                if (index < keys.size - 1) str.append(COMMA_SIGN)
            }
        }
        return str.toString()
    }

    private fun setDefaultTopPadding(view: View) {
        view.setPadding(NO_PADDING, resources.getDimensionPixelOffset(R.dimen.margin_normal), NO_PADDING, NO_PADDING)
    }

    init {
        orientation = VERTICAL
    }

    companion object {
        private const val NO_PADDING = 0
        private const val FIELD_DESCRIPTION_LIMIT = 2
        private const val FIRST_ELEMENT = 0
        private const val PLUS_SIGN = "+ "
        private const val COMMA_SIGN = ", "
    }
}