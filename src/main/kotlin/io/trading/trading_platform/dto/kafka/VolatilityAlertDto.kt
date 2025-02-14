package io.trading.trading_platform.dto.kafka

import java.math.BigDecimal

data class VolatilityAlertDto(
    val symbol: String,
    val windowStart: Long,
    val windowEnd: Long,
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal,
    val volatility: BigDecimal
)