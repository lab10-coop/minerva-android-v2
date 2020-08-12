package minerva.android.widget.clubCard

import android.content.Context
import com.google.gson.annotations.SerializedName
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.full.declaredMemberProperties

class OamtcClubCard(context: Context, path: String, private val credential: Credential) : ClubCard(context, path) {

    override fun getAsHashMap(): HashMap<String, String> {
        return HashMap<String, String>().apply {
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
    }

    private fun getProperFormat(data: Any?): String = when (data) {
        is Long -> DateUtils.getDateFromTimestamp(data, DateUtils.SHORT_DATE_FORMAT)
        else -> data.toString()
    }

    companion object {
        //TODO remove this hardcoded link, when backend will be ready for delivering it
        const val CARD_URL = "http://vc-issuer.dev.lab10.io:8070/detail.svg"
    }
}