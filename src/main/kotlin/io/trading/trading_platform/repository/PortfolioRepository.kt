package io.trading.trading_platform.repository

import io.trading.trading_platform.model.mongo.PortfolioPosition
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PortfolioRepository : ReactiveMongoRepository<PortfolioPosition, String> {
    fun findAllByTraderId(traderId: String): Flux<PortfolioPosition>
    fun findByTraderIdAndStockSymbol(traderId: String, stockSymbol: String): Mono<PortfolioPosition>
}