package io.trading.trading_platform.service.impl

import io.trading.trading_platform.dto.kafka.TradeEventDto
import io.trading.trading_platform.service.KafkaSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderResult

@Service
class KafkaSenderImpl (
    private val reactiveKafkaTemplate: ReactiveKafkaProducerTemplate<String, Any>
) : KafkaSender {
    @Value("\${app.topics.trade.trade-event}")
    private lateinit var tradeTopic: String

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendTradeEvent(event: TradeEventDto) : Mono<SenderResult<Void>> {
        logger.info("sending to topic {}, event {}", tradeTopic, event)
        return reactiveKafkaTemplate.send(tradeTopic, event.symbol, event)
    }
}