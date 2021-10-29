package minerva.android.services.dapps.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.services.dapps.model.Dapp


class OpenDappDialog(private val dapp: Dapp) : DialogFragment() {

    internal lateinit var listener: Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireContext().let {
            with(dapp.openDappDialogData) {
                AlertDialog.Builder(it).apply {
                    setTitle(title)
                    setMessage(instructions)
                    setPositiveButton(R.string.open) { dialog, id ->
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
        val instructions: String = String.Empty
    )

    companion object {
        const val TAG = "OpenDappDialog"
    }
}