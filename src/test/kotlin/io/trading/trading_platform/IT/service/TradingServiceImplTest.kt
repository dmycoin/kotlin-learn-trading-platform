package io.trading.trading_platform.IT.service

import io.trading.trading_platform.IT.AbstractIntegrationTest
import io.trading.trading_platform.model.mongo.PortfolioPosition
import io.trading.trading_platform.model.mongo.Wallet
import io.trading.trading_platform.repository.PortfolioRepository
import io.trading.trading_platform.repository.WalletRepository
import io.trading.trading_platform.service.LeaderboardService
import io.trading.trading_platform.service.impl.TradingServiceImpl
import io.trading.trading_platform.util.RedisKeys
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test

@SpringBootTest
@Testcontainers
class TradingServiceImplTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var tradingService: TradingServiceImpl

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var leaderboardService: LeaderboardService

    @Autowired
    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, String>

    @MockitoSpyBean
    private lateinit var portfolioRepository: PortfolioRepository

    @BeforeTest
    fun setup() {
        Mockito.reset(portfolioRepository)
        walletRepository.deleteAll().block()
        portfolioRepository.deleteAll().block()
        reactiveRedisTemplate.delete(RedisKeys.leaderboardKey()).block()
    }

    @Test
    fun `should delete portfolio position when selling all shares`() {
        // Подготовка
        val traderId = "trader2"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        val initialPosition = PortfolioPosition(
            traderId = traderId,
            stockSymbol = "AAPL",
            quantity = BigDecimal("10"),
            averagePrice = BigDecimal("18.00")
        )

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()
        StepVerifier.create(portfolioRepository.save(initialPosition))
            .expectNextCount(1)
            .verifyComplete()

        // Продаем все акции
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")
        val totalCost = quantity.multiply(price)

        StepVerifier.create(tradingService.sellStock(traderId, "AAPL", quantity, price))
            .verifyComplete()

        // Проверяем, что баланс увеличился
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance.add(totalCost) }
            .verifyComplete()

        // Проверяем, что позиция в портфеле удалена
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `should update leaderboard when buying stock`() {
        // Подготовка
        val traderId = "trader2"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()

        // Покупаем акции
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")
        val totalCost = BigDecimal("200.0")

        StepVerifier.create(tradingService.buyStock(traderId, "AAPL", quantity, price))
            .expectNextMatches {
                it.quantity == quantity &&
                        it.averagePrice == price
            }
            .verifyComplete()

        // Проверяем, что объем торгов обновился в leaderboard
        StepVerifier.create(leaderboardService.getTopTraders(1, LocalDate.now()))
            .expectNextMatches {
                it.traderId == traderId && it.volume == totalCost
            }
            .verifyComplete()
    }

    @Test
    fun `should update leaderboard when selling stock`() {
        // Подготовка
        val traderId = "trader2"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        val initialPosition = PortfolioPosition(
            traderId = traderId,
            stockSymbol = "AAPL",
            quantity = BigDecimal("10"),
            averagePrice = BigDecimal("18.00")
        )

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()
        StepVerifier.create(portfolioRepository.save(initialPosition))
            .expectNextCount(1)
            .verifyComplete()

        // Продаем акции
        val quantity = BigDecimal("5")
        val price = BigDecimal("20.00")
        val totalCost = BigDecimal("100.0")

        StepVerifier.create(tradingService.sellStock(traderId, "AAPL", quantity, price))
            .expectNextMatches {
                it.quantity == initialPosition.quantity.subtract(quantity)
            }
            .verifyComplete()

        // Проверяем, что объем торгов обновился в leaderboard
        StepVerifier.create(leaderboardService.getTopTraders(1, LocalDate.now()))
            .expectNextMatches {
                it.traderId == traderId && it.volume == totalCost
            }
            .verifyComplete()
    }


    @Test
    fun `should handle transaction rollback on insufficient funds`() {
        // Подготовка
        val traderId = "trader1"
        val initialBalance = BigDecimal("100.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()

        // Пытаемся купить акции на сумму больше, чем есть на балансе
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")  // Общая стоимость 200.00

        StepVerifier.create(tradingService.buyStock(traderId, "AAPL", quantity, price))
            .expectError(IllegalArgumentException::class.java)
            .verify()

        // Проверяем, что баланс не изменился (транзакция откатилась)
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance }
            .verifyComplete()

        // Проверяем, что позиция в портфеле не создалась
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `should successfully complete transaction when buying stock`() {
        // Подготовка
        val traderId = "trader2"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()

        // Покупаем акции
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")
        val totalCost = quantity.multiply(price)

        StepVerifier.create(tradingService.buyStock(traderId, "AAPL", quantity, price))
            .expectNextMatches {
                it.quantity == quantity &&
                        it.averagePrice == price
            }
            .verifyComplete()

        // Проверяем, что баланс уменьшился
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance.subtract(totalCost) }
            .verifyComplete()

        // Проверяем, что позиция в портфеле создалась
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextMatches {
                it.quantity == quantity &&
                        it.stockSymbol == "AAPL"
            }
            .verifyComplete()
    }

    @Test
    fun `should rollback wallet changes when portfolio save fails`() {
        // Подготовка
        val traderId = "trader1"
        val symbol = "AAPL"
        val initialBalance = BigDecimal("1000.00")
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")

        // Сохраняем начальный баланс
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()

        // Мокируем ошибку при сохранении портфеля
        Mockito.doReturn(Mono.error<PortfolioPosition>(RuntimeException("Симуляция ошибки сохранения")))
            .`when`(portfolioRepository)
            .save(any())

        // Выполняем операцию покупки
        StepVerifier.create(tradingService.buyStock(traderId, symbol, quantity, price))
            .expectError(RuntimeException::class.java)
            .verify()

        // Проверяем, что баланс в кошельке не изменился (транзакция откатилась)
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance }
            .verifyComplete()

        // Проверяем, что в портфеле нет позиции
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `should rollback wallet changes when portfolio update fails`() {
        // Подготовка
        val traderId = "trader2"
        val symbol = "AAPL"
        val initialBalance = BigDecimal("1000.00")
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")

        // Сохраняем начальный баланс
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()

        // Сохраняем начальную позицию в портфеле
        val initialPosition = PortfolioPosition(
            traderId = traderId,
            stockSymbol = symbol,
            quantity = BigDecimal("5"),
            averagePrice = BigDecimal("18.00")
        )
        StepVerifier.create(portfolioRepository.save(initialPosition))
            .expectNextCount(1)
            .verifyComplete()

        Mockito.doReturn(Mono.error<PortfolioPosition>(RuntimeException("Симуляция ошибки сохранения")))
            .`when`(portfolioRepository)
            .save(any())

        // Выполняем операцию покупки
        StepVerifier.create(tradingService.buyStock(traderId, symbol, quantity, price))
            .expectError(RuntimeException::class.java)
            .verify()

        // Проверяем, что баланс в кошельке не изменился
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance }
            .verifyComplete()

        // Проверяем, что позиция в портфеле осталась без изменений
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextMatches {
                it.quantity == initialPosition.quantity &&
                        it.averagePrice == initialPosition.averagePrice
            }
            .verifyComplete()
    }

    @Test
    fun `should handle concurrent transactions correctly`() {
        val traderId = "trader3"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)

        // Подготовка
        walletRepository.deleteAll()
            .then(walletRepository.save(wallet))
            .block()

        val transaction1 = tradingService.buyStock(traderId, "AAPL", BigDecimal("10"), BigDecimal("20.00"))
        val transaction2 = tradingService.buyStock(traderId, "AAPL", BigDecimal("5"), BigDecimal("30.00"))

        // Запускаем транзакции параллельно
        val combined = Mono.zip(transaction1, transaction2) { _, _ -> }

        // Проверяем результат
        StepVerifier.create(combined)
            .expectError(DataIntegrityViolationException::class.java)
            .verify()
    }

    @Test
    fun `should handle transaction rollback on insufficient shares`() {
        // Подготовка
        val traderId = "trader1"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        val initialPosition = PortfolioPosition(
            traderId = traderId,
            stockSymbol = "AAPL",
            quantity = BigDecimal("5"),
            averagePrice = BigDecimal("18.00")
        )

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()
        StepVerifier.create(portfolioRepository.save(initialPosition))
            .expectNextCount(1)
            .verifyComplete()

        // Пытаемся продать больше акций, чем есть в портфеле
        val quantity = BigDecimal("10")
        val price = BigDecimal("20.00")

        StepVerifier.create(tradingService.sellStock(traderId, "AAPL", quantity, price))
            .expectError(IllegalArgumentException::class.java)
            .verify()

        // Проверяем, что баланс не изменился (транзакция откатилась)
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance }
            .verifyComplete()

        // Проверяем, что позиция в портфеле не изменилась
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextMatches {
                it.quantity == initialPosition.quantity &&
                        it.averagePrice == initialPosition.averagePrice
            }
            .verifyComplete()
    }

    @Test
    fun `should successfully complete transaction when selling stock`() {
        // Подготовка
        val traderId = "trader2"
        val initialBalance = BigDecimal("1000.00")
        val wallet = Wallet(traderId = traderId, balance = initialBalance)
        val initialPosition = PortfolioPosition(
            traderId = traderId,
            stockSymbol = "AAPL",
            quantity = BigDecimal("10"),
            averagePrice = BigDecimal("18.00")
        )

        // Сохраняем начальное состояние
        StepVerifier.create(walletRepository.save(wallet))
            .expectNextCount(1)
            .verifyComplete()
        StepVerifier.create(portfolioRepository.save(initialPosition))
            .expectNextCount(1)
            .verifyComplete()

        // Продаем акции
        val quantity = BigDecimal("5")
        val price = BigDecimal("20.00")
        val totalCost = quantity.multiply(price)

        StepVerifier.create(tradingService.sellStock(traderId, "AAPL", quantity, price))
            .expectNextMatches {
                it.quantity == initialPosition.quantity.subtract(quantity)
            }
            .verifyComplete()

        // Проверяем, что баланс увеличился
        StepVerifier.create(walletRepository.findById(traderId))
            .expectNextMatches { it.balance == initialBalance.add(totalCost) }
            .verifyComplete()

        // Проверяем, что позиция в портфеле уменьшилась
        StepVerifier.create(portfolioRepository.findAllByTraderId(traderId))
            .expectNextMatches {
                it.quantity == initialPosition.quantity.subtract(quantity)
            }
            .verifyComplete()
    }
}