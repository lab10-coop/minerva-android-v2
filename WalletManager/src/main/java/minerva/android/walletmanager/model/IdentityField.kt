package minerva.android.walletmanager.model

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.IdentityField.Companion.ADDRESS_1
import minerva.android.walletmanager.model.IdentityField.Companion.ADDRESS_2
import minerva.android.walletmanager.model.IdentityField.Companion.BIRTH_DATE
import minerva.android.walletmanager.model.IdentityField.Companion.CITY
import minerva.android.walletmanager.model.IdentityField.Companion.COUNTRY
import minerva.android.walletmanager.model.IdentityField.Companion.EMAIL
import minerva.android.walletmanager.model.IdentityField.Companion.NAME
import minerva.android.walletmanager.model.IdentityField.Companion.PHONE_NUMBER
import minerva.android.walletmanager.model.IdentityField.Companion.POSTCODE

@Retention(AnnotationRetention.SOURCE)
@StringDef(NAME, EMAIL, PHONE_NUMBER, BIRTH_DATE, ADDRESS_1, ADDRESS_2, CITY, POSTCODE, COUNTRY)
annotation class IdentityField {
    companion object {
        const val NAME = "name"
        const val EMAIL = "email"
        const val PHONE_NUMBER = "phone_number"
        const val BIRTH_DATE = "birth_date"
        const val ADDRESS_1 = "address_1"
        const val ADDRESS_2 = "address_2"
        const val CITY = "city"
        const val POSTCODE = "postcode"
        const val COUNTRY = "country"
    }
}