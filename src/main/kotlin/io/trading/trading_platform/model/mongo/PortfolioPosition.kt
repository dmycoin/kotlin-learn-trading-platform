package io.trading.trading_platform.model.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "portfolio")
data class PortfolioPosition(
    @Id val id: String? = null,
    val stockSymbol: String, // Символ акции
    val traderId: String, // ID трейдера
    val quantity: BigDecimal, // Количество акций
    val averagePrice: BigDecimal // Средняя цена покупки
) {
    companion object {
        const val STOCK_SYMBOL_FIELD = "stockSymbol"
        const val TRADER_ID_FIELD = "traderId"
        const val QUANTITY_FIELD = "quantity"
        const val AVERAGE_PRICE_FIELD = "averagePrice"
    }
}
