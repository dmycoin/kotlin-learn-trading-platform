package io.trading.trading_platform.service.consumer

import io.trading.trading_platform.dto.kafka.StockPriceDto
import io.trading.trading_platform.service.impl.StockServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class StockPriceConsumer(
    private val stockService: StockServiceImpl,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${app.topics.stock.price}"])
    fun consume(price: StockPriceDto) {
        stockService.updateStockPrice(price).subscribe {
            logger.info("Price updated: $it")
        }
    }
}