package io.trading.trading_platform.repository

import io.trading.trading_platform.model.mongo.StockPrice
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface StockPriceRepository : ReactiveMongoRepository<StockPrice, String> {
    fun findBySymbol(symbol: String): Mono<StockPrice>
}