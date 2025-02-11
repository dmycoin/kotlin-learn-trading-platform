package io.trading.trading_platform.util

import java.time.LocalDate

object RedisKeys {
    const val STOCK_PREFIX = "stock"
    const val TRADER_PREFIX = "trader"

    fun stockKey(symbol: String) = "$STOCK_PREFIX:$symbol"

    fun leaderboardKey(date: LocalDate = LocalDate.now()) = "$TRADER_PREFIX:$date"
}