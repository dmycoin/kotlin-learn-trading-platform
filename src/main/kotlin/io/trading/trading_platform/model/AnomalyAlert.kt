package io.trading.trading_platform.model

import java.time.Instant

data class AnomalyAlert(
    val symbol: String,
    val orderID: String,
    val quantity: Int,
    val timestamp: Instant = Instant.now()
)
