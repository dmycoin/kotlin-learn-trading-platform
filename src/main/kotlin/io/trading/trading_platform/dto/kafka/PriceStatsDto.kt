package io.trading.trading_platform.dto.kafka

import java.math.BigDecimal

data class PriceStatsDto(
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal
)
