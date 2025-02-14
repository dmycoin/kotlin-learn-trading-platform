package io.trading.trading_platform.config.stream

import com.fasterxml.jackson.databind.ObjectMapper
import io.trading.trading_platform.dto.kafka.PriceStatsDto
import io.trading.trading_platform.dto.kafka.StockPriceDto
import io.trading.trading_platform.dto.kafka.VolatilityAlertDto
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import java.math.BigDecimal
import java.time.Duration


@Configuration
@EnableKafkaStreams
@ConditionalOnProperty(value = ["app.kafka.stream.enabled"], havingValue = "true")
class VolatilityAnalyzerConfig(
    private val objectMapper: ObjectMapper
) {
    @Value("\${app.topics.stock.price}")
    private lateinit var tradeTopic: String
    @Value("\${app.topics.alert.volatility-alerts}")
    private lateinit var alertTopic: String

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun volatilityTopology(streamsBuilder: StreamsBuilder) : KStream<String, StockPriceDto> {
        val tradeEventSerde = Serdes.serdeFrom(
            { _, data -> objectMapper.writeValueAsBytes(data)},
            { _, bytes -> objectMapper.readValue(bytes, StockPriceDto::class.java) }
        )

        val priceStatsSerde = Serdes.serdeFrom(
            { _, data -> objectMapper.writeValueAsBytes(data) },
            { _, bytes -> objectMapper.readValue(bytes, PriceStatsDto::class.java) }
        )

        val volatilityAlertSerde = Serdes.serdeFrom(
            { _, data -> objectMapper.writeValueAsBytes(data) },
            { _, bytes -> objectMapper.readValue(bytes, VolatilityAlertDto::class.java) }
        )

        val trades: KStream<String, StockPriceDto> = streamsBuilder.stream(tradeTopic, Consumed.with(Serdes.String(), tradeEventSerde))

        // Группируем события по тикеру акции
        val grouped: KGroupedStream<String, StockPriceDto> = trades.groupBy(
            {key, _ -> key}, Grouped.with(Serdes.String(), tradeEventSerde))

        // Определяем окно (например, tumbling window в 5 минут)
        val windowSize = Duration.ofMinutes(1)
        val windowedStats: KTable<Windowed<String>, PriceStatsDto> = grouped
            .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
            .aggregate(
                { PriceStatsDto(Double.MAX_VALUE.toBigDecimal(), BigDecimal.ZERO) },
                { key, trade, agg ->
                    val newMin = if (trade.price < agg.minPrice) trade.price else agg.minPrice
                    val newMax = if (trade.price > agg.maxPrice) trade.price else agg.maxPrice
                    PriceStatsDto(newMin, newMax)
                },
                Materialized.with(Serdes.String(), priceStatsSerde)
            )

        // Задаем порог волатильности (например, 5%)
        val threshold = BigDecimal(0.05)

        // Фильтруем окна, где волатильность превышает порог, и формируем оповещение
        val alerts: KStream<String, VolatilityAlertDto> = windowedStats
            .toStream()
            .filter { _, stats ->
                stats.minPrice != Double.MAX_VALUE.toBigDecimal() && stats.maxPrice != Double.MIN_VALUE.toBigDecimal() &&
                        stats.minPrice > BigDecimal.ZERO && ((stats.maxPrice - stats.minPrice) / stats.minPrice) >= threshold
            }
            .map { windowedKey, stats ->
                val symbol = windowedKey.key()
                val alert = VolatilityAlertDto(
                    symbol = symbol,
                    windowStart = windowedKey.window().start(),
                    windowEnd = windowedKey.window().end(),
                    minPrice = stats.minPrice,
                    maxPrice = stats.maxPrice,
                    volatility = (stats.maxPrice - stats.minPrice) / stats.minPrice
                )
                KeyValue.pair(symbol, alert)
            }

        // Пишем оповещения в топик "volatility-alerts"
        alerts.peek { _, alert ->
            logger.info("🚨 Alert event: $alert")
        }.to(alertTopic, Produced.with(Serdes.String(), volatilityAlertSerde))

        return trades
    }
}