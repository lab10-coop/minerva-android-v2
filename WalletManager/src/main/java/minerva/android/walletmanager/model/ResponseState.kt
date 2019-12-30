package minerva.android.walletmanager.model

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    ResponseState.ERROR,
    ResponseState.SUCCESS
)

annotation class ResponseState {
    companion object {
        const val ERROR = "error"
        const val SUCCESS = "success"
    }
}