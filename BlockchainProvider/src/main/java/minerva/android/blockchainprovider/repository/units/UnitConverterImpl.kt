package minerva.android.blockchainprovider.repository.units

import org.web3j.utils.Convert
import java.math.BigDecimal

class UnitConverterImpl : UnitConverter {
    override fun toGwei(amount: BigDecimal): BigDecimal = Convert.toWei(amount, Convert.Unit.GWEI)
    override fun fromWei(value: BigDecimal): BigDecimal = Convert.fromWei(value, Convert.Unit.GWEI)
    override fun toEther(value: BigDecimal): BigDecimal = Convert.fromWei(value, Convert.Unit.ETHER)
    override fun fromGwei(amount: BigDecimal): BigDecimal = Convert.fromWei(amount, Convert.Unit.ETHER)
}