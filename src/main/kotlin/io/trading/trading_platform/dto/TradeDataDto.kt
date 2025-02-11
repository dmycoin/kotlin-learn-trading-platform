package io.trading.trading_platform.dto

data class TradeDataDto (
    val s: String,      // symbol
    val p: Double,      // price
    val t: Long,        // timestamp в миллисекундах
    val c: List<String>? = null,
    val v: Int? = null
)