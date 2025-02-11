package io.trading.trading_platform.IT.service

import io.trading.trading_platform.IT.AbstractIntegrationTest
import io.trading.trading_platform.dto.TopTraderDto
import io.trading.trading_platform.service.impl.LeaderboardServiceImpl
import io.trading.trading_platform.util.RedisKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test

@SpringBootTest
@Testcontainers
@DirtiesContext
class LeaderboardServiceImplTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var leaderboardService: LeaderboardServiceImpl

    @Autowired
    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>

    @BeforeEach
    fun init() {
        reactiveRedisTemplate.delete(RedisKeys.leaderboardKey()).block()
    }

    @Test
    @Order(1)
    fun `test updateTradeVolume`() {
        val traderId = "trader123"
        val volume = BigDecimal("100.50")

        val result = leaderboardService.updateTradeVolume(traderId, volume)

        StepVerifier.create(result)
            .expectNext(BigDecimal("100.5"))
            .verifyComplete()
    }

    @Test
    @Order(2)
    fun `test getTopTraders`() {
        val traderId = "trader123"
        val volume = BigDecimal("100.5")
        val date = LocalDate.now()

        leaderboardService.updateTradeVolume(traderId, volume).block()

        val result = leaderboardService.getTopTraders(1, date)

        StepVerifier.create(result)
            .assertNext { topTrader: TopTraderDto ->
                assertEquals(traderId, topTrader.traderId)
                assertEquals(volume, topTrader.volume)
            }
            .verifyComplete()
    }
}