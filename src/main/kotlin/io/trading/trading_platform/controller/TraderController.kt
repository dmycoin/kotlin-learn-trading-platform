package io.trading.trading_platform.controller

import io.trading.trading_platform.dto.PortfolioPositionDto
import io.trading.trading_platform.dto.WalletDto
import io.trading.trading_platform.service.TradingService
import io.trading.trading_platform.service.WalletService
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

@RestController
@RequestMapping("/api/trader")
class TraderController (
    private val walletService: WalletService,
    private val tradingService: TradingService
) {
    @GetMapping("/{traderId}/wallet")
    fun getWallet(@PathVariable traderId : String) : Mono<WalletDto> {
        return walletService.getWallet(traderId)
            .map { WalletDto(traderId = it.traderId, balance = it.balance) }
            .onErrorResume(NotFoundException::class.java) {
                Mono.error(
                    ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                    )
                )
            }
    }

    // Получение портфеля трейдера
    @GetMapping("/{traderId}/portfolio")
    fun getPortfolio(@PathVariable traderId: String): Flux<PortfolioPositionDto> =
        tradingService.getPortfolio(traderId)
            .map {
                PortfolioPositionDto(
                    traderId = it.traderId,
                    stockSymbol = it.stockSymbol,
                    quantity = it.quantity,
                    averagePrice = it.averagePrice
                )
            }

    // Покупка акций
    @PostMapping("/{traderId}/buy")
    fun buyStock(
        @PathVariable traderId: String,
        @RequestParam stockSymbol: String,
        @RequestParam quantity: BigDecimal,
        @RequestParam price: BigDecimal
    ): Mono<ResponseEntity<PortfolioPositionDto>> =
        tradingService.buyStock(traderId, stockSymbol, quantity, price)
            .map {
                PortfolioPositionDto(
                    traderId = it.traderId,
                    stockSymbol = it.stockSymbol,
                    quantity = it.quantity,
                    averagePrice = it.averagePrice
                )
            }
            .map { ResponseEntity(it, HttpStatus.OK) }
            .onErrorResume { Mono.just(ResponseEntity.badRequest().build()) }

    // Продажа акций
    @PostMapping("/{traderId}/sell")
    fun sellStock(
        @PathVariable traderId: String,
        @RequestParam stockSymbol: String,
        @RequestParam quantity: BigDecimal,
        @RequestParam price: BigDecimal
    ): Mono<ResponseEntity<PortfolioPositionDto>> =
        tradingService.sellStock(traderId, stockSymbol, quantity, price)
            .map {
                PortfolioPositionDto(
                    traderId = it.traderId,
                    stockSymbol = it.stockSymbol,
                    quantity = it.quantity,
                    averagePrice = it.averagePrice
                )
            }
            .map { ResponseEntity(it, HttpStatus.OK) }
            .onErrorResume { Mono.just(ResponseEntity.badRequest().build()) }
}