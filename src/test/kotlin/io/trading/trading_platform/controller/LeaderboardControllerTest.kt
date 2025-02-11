package io.trading.trading_platform.controller

import io.trading.trading_platform.dto.TopTraderDto
import io.trading.trading_platform.service.LeaderboardService
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import java.math.BigDecimal
import kotlin.test.Test

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [LeaderboardController::class])
class LeaderboardControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var leaderboardService: LeaderboardService

    @Test
    fun `test getTopTraders`() {
        val topTraders = listOf(
            TopTraderDto("trader1", BigDecimal("1000")),
            TopTraderDto("trader2", BigDecimal("900"))
        )

        `when`(leaderboardService.getTopTraders(10)).thenReturn(Flux.fromIterable(topTraders))

        webTestClient.get().uri("/api/leaderboard")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TopTraderDto::class.java)
            .hasSize(2)
            .contains(*topTraders.toTypedArray())

        verify(leaderboardService, times(1)).getTopTraders(10)
    }


}