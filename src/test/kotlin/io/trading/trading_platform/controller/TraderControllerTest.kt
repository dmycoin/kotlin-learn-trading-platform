package io.trading.trading_platform.controller

import io.mockk.coEvery
import io.mockk.mockk
import io.trading.trading_platform.model.mongo.PortfolioPosition
import io.trading.trading_platform.model.mongo.Wallet
import io.trading.trading_platform.service.TradingService
import io.trading.trading_platform.service.WalletService
import io.trading.trading_platform.service.impl.WalletServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import kotlin.test.Test

@WebFluxTest(TraderController::class)
class TraderControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var walletService: WalletService

    @MockitoBean
    private lateinit var tradingService: TradingService

    @BeforeEach
    fun setUp() {
        walletService = mockk()
    }

    @Test
    fun `test getWallet API`() {
        val traderId = "trader123"
        val expectedBalance = BigDecimal("100.00")

        // Используем relaxed мок для упрощения
        val relaxedWalletService = mockk<WalletServiceImpl>(relaxed = true)

        // Настройка мока с явным указанием возвращаемого значения
            coEvery {
            relaxedWalletService.getWallet(match { it == traderId })
        } returns Mono.just(Wallet(traderId, expectedBalance))

        // Пересоздаем WebTestClient с новым моком
        webTestClient = WebTestClient.bindToController(TraderController(relaxedWalletService, tradingService)).build()

        // 2. Выполнение запроса с диагностикой
        webTestClient.get()
            .uri("/api/trader/$traderId/wallet")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith { result ->
                // 3. Вывод информации об ошибке
                if (result.status != HttpStatus.OK) {
                    println("Response body: ${String(result.responseBodyContent!!)}")
                }
            }
            .jsonPath("$.traderId").isEqualTo(traderId)
            .jsonPath("$.balance").isEqualTo(expectedBalance)
    }

    @Test
    fun `test getPortfolio API`() {
        val traderId = "trader123"
        val portfolioPosition1 = PortfolioPosition(traderId, "AAPL", traderId, BigDecimal("10"), BigDecimal("150.00"))
        val portfolioPosition2 = PortfolioPosition(traderId, "GOOGL", traderId, BigDecimal("5"), BigDecimal("2000.00"))

        // Используем relaxed мок для упрощения
        val relaxedTradingService = mockk<TradingService>(relaxed = true)

        // Настройка мока с явным указанием возвращаемого значения
        coEvery {
            relaxedTradingService.getPortfolio(match { it == traderId })
        } returns Flux.just(portfolioPosition1, portfolioPosition2)

        // Пересоздаем WebTestClient с новым моком
        webTestClient = WebTestClient.bindToController(TraderController(walletService, relaxedTradingService)).build()

        // Выполнение запроса с диагностикой
        webTestClient.get()
            .uri("/api/trader/$traderId/portfolio")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].traderId").isEqualTo(traderId)
            .jsonPath("$[0].stockSymbol").isEqualTo("AAPL")
            .jsonPath("$[0].quantity").isEqualTo(10)
            .jsonPath("$[0].averagePrice").isEqualTo(150.00)
            .jsonPath("$[1].traderId").isEqualTo(traderId)
            .jsonPath("$[1].stockSymbol").isEqualTo("GOOGL")
            .jsonPath("$[1].quantity").isEqualTo(5)
            .jsonPath("$[1].averagePrice").isEqualTo(2000.00)
    }

    @Test
    fun `test buyStock API`() {
        val traderId = "trader123"
        val stockSymbol = "AAPL"
        val quantity = BigDecimal("10")
        val price = BigDecimal("150.00")
        val portfolioPosition = PortfolioPosition(traderId, stockSymbol, traderId, quantity, price)

        // Используем relaxed мок для упрощения
        val relaxedTradingService = mockk<TradingService>(relaxed = true)

        // Настройка мока с явным указанием возвращаемого значения
        coEvery {
            relaxedTradingService.buyStock(match { it == traderId }, match { it == stockSymbol }, match { it == quantity }, match { it == price })
        } returns Mono.just(portfolioPosition)

        // Пересоздаем WebTestClient с новым моком
        webTestClient = WebTestClient.bindToController(TraderController(walletService, relaxedTradingService)).build()

        // Выполнение запроса с диагностикой
        webTestClient.post()
            .uri("/api/trader/$traderId/buy?stockSymbol=$stockSymbol&quantity=$quantity&price=$price")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.traderId").isEqualTo(traderId)
            .jsonPath("$.stockSymbol").isEqualTo(stockSymbol)
            .jsonPath("$.quantity").isEqualTo(quantity)
            .jsonPath("$.averagePrice").isEqualTo(price)
    }

    @Test
    fun `test sellStock API`() {
        val traderId = "trader123"
        val stockSymbol = "AAPL"
        val quantity = BigDecimal("10")
        val price = BigDecimal("150.00")
        val portfolioPosition = PortfolioPosition(traderId, stockSymbol, traderId, quantity, price)

        // Используем relaxed мок для упрощения
        val relaxedTradingService = mockk<TradingService>(relaxed = true)

        // Настройка мока с явным указанием возвращаемого значения
        coEvery {
            relaxedTradingService.sellStock(match { it == traderId }, match { it == stockSymbol }, match { it == quantity }, match { it == price })
        } returns Mono.just(portfolioPosition)

        // Пересоздаем WebTestClient с новым моком
        webTestClient = WebTestClient.bindToController(TraderController(walletService, relaxedTradingService)).build()

        // Выполнение запроса с диагностикой
        webTestClient.post()
            .uri("/api/trader/$traderId/sell?stockSymbol=$stockSymbol&quantity=$quantity&price=$price")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.traderId").isEqualTo(traderId)
            .jsonPath("$.stockSymbol").isEqualTo(stockSymbol)
            .jsonPath("$.quantity").isEqualTo(quantity)
            .jsonPath("$.averagePrice").isEqualTo(price)
    }


}