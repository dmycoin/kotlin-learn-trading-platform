package io.trading.trading_platform.service

import io.trading.trading_platform.dto.kafka.StockPriceDto
import io.trading.trading_platform.model.mongo.StockPrice
import reactor.core.publisher.Mono

interface StockService {
    fun updateStockPrice(stockPriceDto: StockPriceDto): Mono<StockPrice>
    fun getStockPrice(symbol: String): Mono<StockPrice>
}