package minerva.android.main.listener

interface BiometricDialogCallback {
    fun showBiometricDialog(onSuccessAction: () -> Unit)
}