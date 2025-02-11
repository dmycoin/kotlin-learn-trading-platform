package io.trading.trading_platform.model.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "stock")
data class StockPrice(
    @Id val symbol: String,
    val price: BigDecimal,
    val timestamp: Instant = Instant.now()
)
