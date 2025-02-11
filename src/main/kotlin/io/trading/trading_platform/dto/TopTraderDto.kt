package io.trading.trading_platform.dto

import java.math.BigDecimal

data class TopTraderDto (
    val traderId: String,
    val volume: BigDecimal
)
