package io.trading.trading_platform.controller

import io.trading.trading_platform.dto.TopTraderDto
import io.trading.trading_platform.service.LeaderboardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardController(
    private var leaderboardService: LeaderboardService
) {
    /**
     * Получение топ-N трейдеров по объёму торгов за текущую дату.
     * @param limit количество записей в лидерборде (по умолчанию 10)
     */
    @GetMapping
    fun getTopTraders(@RequestParam(required = false, defaultValue = "10") limit: Long): ResponseEntity<Flux<TopTraderDto>> {
        return ResponseEntity(leaderboardService.getTopTraders(limit), HttpStatus.OK)
    }


}