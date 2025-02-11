package io.trading.trading_platform.model.elk

import io.trading.trading_platform.model.OrderStatus
import io.trading.trading_platform.model.OrderType
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.math.BigDecimal
import java.time.Instant

@Document(indexName = "trade_order")
data class TradeOrder(
    @Id val id: String,
    val symbol: String,
    val userId: String,
    val quantity: Int,
    val orderType: OrderType,
    val status: OrderStatus = OrderStatus.PENDING,
    val executedPrice: BigDecimal,
    val timestamp: Instant = Instant.now()
)
