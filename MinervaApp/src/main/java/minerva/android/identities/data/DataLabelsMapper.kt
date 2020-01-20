package minerva.android.identities.data

import android.content.Context
import minerva.android.R
import minerva.android.walletmanager.model.IdentityField.Companion.ADDRESS_1
import minerva.android.walletmanager.model.IdentityField.Companion.ADDRESS_2
import minerva.android.walletmanager.model.IdentityField.Companion.BIRTH_DATE
import minerva.android.walletmanager.model.IdentityField.Companion.CITY
import minerva.android.walletmanager.model.IdentityField.Companion.COUNTRY
import minerva.android.walletmanager.model.IdentityField.Companion.EMAIL
import minerva.android.walletmanager.model.IdentityField.Companion.NAME
import minerva.android.walletmanager.model.IdentityField.Companion.PHONE_NUMBER
import minerva.android.walletmanager.model.IdentityField.Companion.POSTCODE
import minerva.android.kotlinUtils.Empty


//TODO labels will be changed to dynamic in phase 2.

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
}