package io.trading.trading_platform.service.impl

import io.trading.trading_platform.dto.TopTraderDto
import io.trading.trading_platform.service.LeaderboardService
import io.trading.trading_platform.util.RedisKeys
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

@Service
class LeaderboardServiceImpl(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) : LeaderboardService {

    override fun updateTradeVolume(traderId: String, volume: BigDecimal): Mono<BigDecimal> {
        val key = RedisKeys.leaderboardKey()
        return reactiveRedisTemplate.opsForZSet().incrementScore(key, traderId, volume.toDouble()).map { BigDecimal.valueOf(it) }
    }

    override fun getTopTraders(limit: Long, date: LocalDate): Flux<TopTraderDto> {
        val key = RedisKeys.leaderboardKey(date)
        val range = Range.closed(0, limit - 1)
        return reactiveRedisTemplate.opsForZSet().reverseRangeWithScores(key, range)
            .map { tuple -> TopTraderDto(traderId = tuple.value!!, volume = BigDecimal.valueOf(tuple.score ?: 0.0)) }
    }

}