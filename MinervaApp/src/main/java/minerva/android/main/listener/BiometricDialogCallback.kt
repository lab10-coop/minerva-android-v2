package minerva.android.main.listener

interface BiometricDialogCallback {
    fun showBiometricDialog(onFailMessage: String, onErrorMessage: String, onSuccessAction: () -> Unit)
}