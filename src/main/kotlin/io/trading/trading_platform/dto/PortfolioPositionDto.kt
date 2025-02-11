package io.trading.trading_platform.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal

data class PortfolioPositionDto(
    val stockSymbol: String, // Символ акции
    val traderId: String, // ID трейдера
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val quantity: BigDecimal, // Количество акций
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val averagePrice: BigDecimal // Средняя цена покупки
)
