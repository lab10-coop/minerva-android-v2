package minerva.android.identities.data

import android.content.Context
import minerva.android.R
import minerva.android.kotlinUtils.Empty


//TODO labels will be changed to dynamic in phase 2.
val IDENTITY_DATA_LIST: List<String>
    get() = listOf(
        NAME,
        EMAIL,
        PHONE_NUMBER,
        BIRTH_DATE,
        ADDRESS_1,
        ADDRESS_2,
        CITY,
        POSTCODE,
        COUNTRY
    )

fun getIdentityDataLabel(context: Context, key: String): String {
    context.resources.getStringArray(R.array.identities_data_labels).apply {
        return when (key) {
            NAME -> this[0]
            EMAIL -> this[1]
            PHONE_NUMBER -> this[2]
            BIRTH_DATE -> this[3]
            ADDRESS_1 -> this[4]
            ADDRESS_2 -> this[5]
            CITY -> this[6]
            POSTCODE -> this[7]
            COUNTRY -> this[8]
            else -> String.Empty
        }
    }
    return String.Empty
}

private const val NAME = "name"
private const val EMAIL = "email"
private const val PHONE_NUMBER = "phone_number"
private const val BIRTH_DATE = "birth_date"
private const val ADDRESS_1 = "address_1"
private const val ADDRESS_2 = "address_2"
private const val CITY = "city"
private const val POSTCODE = "postcode"
private const val COUNTRY = "country"