package minerva.android.services.dapps.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.services.dapps.model.Dapp
import kotlin.reflect.jvm.jvmName


class OpenDappDialog(private val dapp: Dapp) : DialogFragment() {

    internal lateinit var listener: Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireContext().let {
            with(dapp.openDappDialogData) {
                AlertDialog.Builder(it).apply {
                    setTitle(title)
                    setMessage(description)
                    setPositiveButton(confirm) { dialog, id ->
                        listener.onConfirm(Listener.OnConfirmData(url))
                    }
                    setNegativeButton(R.string.cancel) { dialog, id ->
                        listener.onCancel()
                    }

                }.create()
            }
        }
    }

    interface Listener {
        fun onConfirm(onConfirmData: OnConfirmData)
        fun onCancel()

        data class OnConfirmData(
            val url: String
        )
    }

    data class Data(
        val title: String = String.Empty,
        val url: String = String.Empty,
        @StringRes val description: Int = Int.EmptyResource,
        @StringRes val confirm: Int = Int.EmptyResource
    )

    companion object {
        const val TAG = "OpenDappDialog"
    }
}