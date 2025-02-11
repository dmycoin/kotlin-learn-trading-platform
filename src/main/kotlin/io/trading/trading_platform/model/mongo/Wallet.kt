package io.trading.trading_platform.model.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "wallet")
data class Wallet (
    @Id val traderId: String,
    val balance: BigDecimal,
    @Version
    val version: Long = 0
) {
    init {
        require(balance >= BigDecimal.ZERO) { "Balance must be non-negative" }
    }

    companion object {
        const val BALANCE_FIELD = "balance"
        const val ID_FIELD = "_id"
    }
}