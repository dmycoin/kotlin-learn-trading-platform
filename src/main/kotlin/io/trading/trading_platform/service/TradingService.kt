package io.trading.trading_platform.service

import io.trading.trading_platform.model.mongo.PortfolioPosition
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface TradingService {
    fun buyStock(traderId: String, symbol: String, quantity: BigDecimal, price: BigDecimal) : Mono<PortfolioPosition>
    fun sellStock(traderId: String, symbol: String, quantity: BigDecimal, price: BigDecimal): Mono<PortfolioPosition>
    fun getPortfolio(traderId: String) : Flux<PortfolioPosition>
}