package minerva.android.widget.clubCard

import android.content.Context
import com.google.gson.annotations.SerializedName
import minerva.android.walletmanager.model.Credential
import minerva.android.kotlinUtils.DateUtils
import kotlin.reflect.full.declaredMemberProperties

class OamtcClubCard(context: Context, private val credential: Credential) : ClubCard(context, credential.cardUrl) {

    override fun getAsHashMap(): HashMap<String, String> =
        HashMap<String, String>().apply {
            for (prop in Credential::class.declaredMemberProperties) {
                Credential::class.java.getDeclaredField(prop.name).let { field ->
                    field.getAnnotation(SerializedName::class.java)?.let { fieldSerializedName ->
                        this[fieldSerializedName.value] = getProperFormat(prop.get(credential))
                    } ?: run {
                        this[prop.name] = getProperFormat(prop.get(credential))
                    }
                }
            }
        }

    private fun getProperFormat(data: Any?): String = when (data) {
        is Long -> DateUtils.getDateFromTimestamp(data, DateUtils.SHORT_DATE_FORMAT)
        else -> data.toString()
    }
}