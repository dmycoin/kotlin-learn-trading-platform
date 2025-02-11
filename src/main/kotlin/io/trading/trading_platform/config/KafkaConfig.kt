package io.trading.trading_platform.config

import io.trading.trading_platform.model.elk.TradeOrder
import io.trading.trading_platform.model.mongo.StockPrice
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.support.serializer.JsonSerde

@Configuration
@EnableKafka
//@EnableKafkaStreams
class KafkaConfig {
    @Bean
    fun defaultKafkaStreamsConfig(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${spring.kafka.streams.application-id}") appId: String
    ): KafkaStreamsConfiguration {
        val props = mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to appId,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.StringSerde::class.java.name,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to JsonSerde::class.java.name,
            StreamsConfig.PROCESSING_GUARANTEE_CONFIG to StreamsConfig.EXACTLY_ONCE_V2
        )
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun stockPriceSerde(): JsonSerde<StockPrice> = JsonSerde(StockPrice::class.java)

    @Bean
    fun tradeOrderSerde(): JsonSerde<TradeOrder> = JsonSerde(TradeOrder::class.java)
}