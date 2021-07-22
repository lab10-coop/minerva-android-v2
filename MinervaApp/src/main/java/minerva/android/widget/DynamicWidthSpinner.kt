package minerva.android.widget

import android.content.Context
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import androidx.appcompat.widget.AppCompatSpinner

class DynamicWidthSpinner : AppCompatSpinner {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    override fun setAdapter(adapter: SpinnerAdapter?) {
        super.setAdapter(if (adapter != null) WrapperSpinnerAdapter(adapter) else null)
    }

    inner class WrapperSpinnerAdapter(private val baseAdapter: SpinnerAdapter) : SpinnerAdapter {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return baseAdapter.getView(selectedItemPosition, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return baseAdapter.getDropDownView(position, convertView, parent)
        }

        override fun getCount(): Int = baseAdapter.count
        override fun getItem(position: Int): Any = baseAdapter.getItem(position)
        override fun getItemId(position: Int): Long = baseAdapter.getItemId(position)
        override fun getItemViewType(position: Int): Int = baseAdapter.getItemViewType(position)
        override fun getViewTypeCount(): Int = baseAdapter.viewTypeCount
        override fun hasStableIds(): Boolean = baseAdapter.hasStableIds()
        override fun isEmpty(): Boolean = baseAdapter.isEmpty

        override fun registerDataSetObserver(observer: DataSetObserver) {
            baseAdapter.registerDataSetObserver(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            baseAdapter.unregisterDataSetObserver(observer)
        }
    }
}