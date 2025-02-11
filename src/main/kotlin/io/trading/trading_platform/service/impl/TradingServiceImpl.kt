package io.trading.trading_platform.service.impl

import io.trading.trading_platform.exception.EntityNotFoundException
import io.trading.trading_platform.model.mongo.PortfolioPosition
import io.trading.trading_platform.repository.PortfolioRepository
import io.trading.trading_platform.service.LeaderboardService
import io.trading.trading_platform.service.TradingService
import io.trading.trading_platform.service.WalletService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class TradingServiceImpl (
    private val portfRepository: PortfolioRepository,
    private val walletRepositoryService: WalletService,
    private val leaderboardService: LeaderboardService
) : TradingService {

    private val logger = LoggerFactory.getLogger(javaClass)

    //TODO Сейчас при конкурентных операциях выбрасывается WriteConflict и транзакция откатывается. Возможно нужно подумать, как обрабатывать такие ситуации(ретраи + новая транзакция)
    @Transactional
    override fun buyStock(traderId: String, symbol: String, quantity: BigDecimal, price: BigDecimal) : Mono<PortfolioPosition> {
        require(quantity > BigDecimal.ZERO) { "Quantity must be positive" }
        require(price >= BigDecimal.ZERO) { "Price cannot be negative" }

        val totalCost = quantity.multiply(price)

        return walletRepositoryService.withdrawAmount(traderId, totalCost)
            .then(
                portfRepository.findByTraderIdAndStockSymbol(traderId, symbol)
                    .flatMap { existingPosition ->
                        // Обновление существующей позиции
                        val newQuantity = existingPosition.quantity.add(quantity)
                        val totalValue = existingPosition.averagePrice
                            .multiply(existingPosition.quantity)
                            .add(totalCost)
                        val newAveragePrice = totalValue.divide(newQuantity, 4, RoundingMode.HALF_EVEN)

                        portfRepository.save(
                            existingPosition.copy(
                                quantity = newQuantity,
                                averagePrice = newAveragePrice
                            )
                        )
                    }
                    .switchIfEmpty(
                        // Создание новой позиции
                        portfRepository.save(
                            PortfolioPosition(
                                traderId = traderId,
                                stockSymbol = symbol,
                                quantity = quantity,
                                averagePrice = price
                            )
                        )
                    )
            )
            .flatMap { savedPosition ->
                leaderboardService.updateTradeVolume(traderId, totalCost)
                    .thenReturn(savedPosition)
                    .doOnSubscribe {
                        logger.info("Обновление лидерборда(покупка) для traderId = $traderId")
                    }
            }
    }

    @Transactional
    override fun sellStock(traderId: String, symbol: String, quantity: BigDecimal, price: BigDecimal) : Mono<PortfolioPosition> {
        require(quantity > BigDecimal.ZERO) { "Quantity must be positive" }
        require(price >= BigDecimal.ZERO) { "Price cannot be negative" }

        val totalCost = quantity.multiply(price)

        return portfRepository.findByTraderIdAndStockSymbol(traderId, symbol)
            .switchIfEmpty(Mono.error(NoSuchElementException("Position for symbol $symbol not found for traderId $traderId")))
            .flatMap { position ->
                if (position.quantity < quantity) {
                    Mono.error(IllegalArgumentException("Not enough shares to sell"))
                } else {
                    val newQuantity = position.quantity.subtract(quantity)
                    val updatePortfolio = if (newQuantity == BigDecimal.ZERO) {
                        portfRepository.delete(position).then(Mono.empty())
                    } else {
                        portfRepository.save(position.copy(quantity = newQuantity))
                    }

                    updatePortfolio
                        .flatMap { portfolio ->
                            walletRepositoryService.depositAmount(traderId, totalCost)
                                .then(leaderboardService.updateTradeVolume(traderId, totalCost))
                                .thenReturn(portfolio)
                        }
                        .switchIfEmpty(
                            walletRepositoryService.depositAmount(traderId, totalCost)
                                .then(leaderboardService.updateTradeVolume(traderId, totalCost))
                                .then(Mono.empty())
                        )
                        .doOnSubscribe {
                            logger.info("Обновление лидерборда(продажа) для traderId = $traderId")
                        }
                }
            }
    }

    override fun getPortfolio(traderId: String) : Flux<PortfolioPosition> {
        return portfRepository.findAllByTraderId(traderId).switchIfEmpty(Mono.error(EntityNotFoundException("Portfolio not found for traderId = $traderId")))
    }


}