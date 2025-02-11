package io.trading.trading_platform.service

import io.trading.trading_platform.dto.TopTraderDto
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Сервис лидерборда, который обновляет торговый объём и возвращает топ трейдеров
 */
interface LeaderboardService {

    /**
     * Обновление торгового объёма трейдера.
     * @param traderId Идентификатор трейдера.
     * @param volume Текущий объём сделки (например, сумма сделки).
     * @return Новое значение торгового объёма (score) для трейдера.
     */
    fun updateTradeVolume(traderId: String, volume: BigDecimal) : Mono<BigDecimal>

    /**
     * Получение топ-N трейдеров по торговому объёму за текущую дату.
     * @param limit Количество записей (например, топ-10).
     * @param date Дата лидерборда (по умолчанию сегодня).
     * @return топ-N трейдеров
     */
    fun getTopTraders(limit: Long, date: LocalDate = LocalDate.now()) : Flux<TopTraderDto>
}