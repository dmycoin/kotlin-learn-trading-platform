package io.trading.trading_platform.IT.service

import io.trading.trading_platform.IT.AbstractIntegrationTest
import io.trading.trading_platform.dto.kafka.StockPriceDto
import io.trading.trading_platform.model.mongo.StockPrice
import io.trading.trading_platform.repository.StockPriceRepository
import io.trading.trading_platform.service.impl.StockServiceImpl
import io.trading.trading_platform.util.RedisKeys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest
@Testcontainers
class StockServiceImplTest @Autowired constructor(
    private val stockService: StockServiceImpl,
    private val stockRepository: StockPriceRepository,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>
) : AbstractIntegrationTest() {

    companion object {
        private const val SYMBOL = "AAPL"
    }

    @BeforeEach
    fun before() {
        stockRepository.deleteAll().block()
        reactiveRedisTemplate.delete(RedisKeys.stockKey(SYMBOL)).block()
    }

    @Test
    fun `test updateStockPrice`() {
        val stockPriceDto = StockPriceDto(SYMBOL, BigDecimal("150.00"), Instant.now())

        val result = stockService.updateStockPrice(stockPriceDto)

        StepVerifier.create(result)
            .expectNextMatches { it.symbol == SYMBOL && it.price == BigDecimal("150.00") }
            .verifyComplete()

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(RedisKeys.stockKey(SYMBOL)).map { it as StockPriceDto })
            .expectNextMatches { it is StockPriceDto && it.symbol == SYMBOL && it.price == BigDecimal("150.00") }
            .verifyComplete()
    }

    @Test
    fun `test getStockPrice`() {
        val stockPrice = StockPrice(SYMBOL, BigDecimal("150.00"), Instant.now())
        stockRepository.save(stockPrice).block()

        val result = stockService.getStockPrice(SYMBOL)

        StepVerifier.create(result)
            .expectNextMatches { it.symbol == SYMBOL && it.price == BigDecimal("150.00") }
            .verifyComplete()
    }

    @Test
    fun `test getStockPriceFromRedis`() {
        val stockPrice = StockPriceDto(SYMBOL, BigDecimal("150.00"), Instant.now())
        reactiveRedisTemplate.opsForValue().set(RedisKeys.stockKey(stockPrice.symbol), stockPrice).block()

        val result = stockService.getStockPrice(SYMBOL)

        StepVerifier.create(result)
            .expectNextMatches { it.symbol == SYMBOL && it.price == BigDecimal("150.00") }
            .verifyComplete()
    }

    @Test
    fun `test update existing StockPrice`() {
        val initialStockPrice = StockPrice(SYMBOL, BigDecimal("150.00"), Instant.now())
        stockRepository.save(initialStockPrice).block()

        val updatedStockPriceDto = StockPriceDto(SYMBOL, BigDecimal("155.00"), Instant.now())
        val result = stockService.updateStockPrice(updatedStockPriceDto)

        StepVerifier.create(result)
            .expectNextMatches { it.symbol == SYMBOL && it.price == BigDecimal("155.00") }
            .verifyComplete()

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(RedisKeys.stockKey(SYMBOL)))
            .expectNextMatches { it is StockPriceDto && it.symbol == SYMBOL && it.price == BigDecimal("155.00") }
            .verifyComplete()
    }

    @Test
    fun `test getStockPrice with non-existent symbol`() {
        val result = stockService.getStockPrice("NON_EXISTENT")

        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete()
    }
}