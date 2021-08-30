package minerva.android.blockchainprovider.repository.units

import java.math.BigDecimal

interface UnitConverter {
    fun toGwei(amount: BigDecimal): BigDecimal
    fun fromWei(value: BigDecimal): BigDecimal
    fun toEther(value: BigDecimal): BigDecimal
    fun fromGwei(amount: BigDecimal): BigDecimal
}