package io.trading.trading_platform.dto.kafka

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
data class StockPriceDto(
    val symbol: String,
    val price: BigDecimal,
    val timestamp: Instant
)
