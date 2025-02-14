package io.trading.trading_platform.dto.kafka

import java.math.BigDecimal
import java.time.Instant

data class TradeEventDto(
    val symbol: String,
    val price: BigDecimal,
    val volume: BigDecimal,
    val timestamp: Instant = Instant.now()
)
