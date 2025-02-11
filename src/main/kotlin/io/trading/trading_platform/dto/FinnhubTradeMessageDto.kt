package io.trading.trading_platform.dto

data class FinnhubTradeMessageDto(
    val type: String,
    val data: List<TradeDataDto>
)
