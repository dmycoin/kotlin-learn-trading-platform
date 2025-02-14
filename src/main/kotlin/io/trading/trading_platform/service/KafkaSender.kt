package io.trading.trading_platform.service

import io.trading.trading_platform.dto.kafka.TradeEventDto
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderResult

interface KafkaSender {
    fun sendTradeEvent(event: TradeEventDto): Mono<SenderResult<Void>>
}