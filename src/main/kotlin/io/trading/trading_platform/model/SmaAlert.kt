package io.trading.trading_platform.model

import java.math.BigDecimal
import java.time.Instant

data class SmaAlert(
    val symbol: String,
    val sma: BigDecimal,
    val price: BigDecimal,
    val deviation: BigDecimal,
    val timestamp: Instant = Instant.now()
)
