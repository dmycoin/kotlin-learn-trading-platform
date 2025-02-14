package io.trading.trading_platform.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.kafka.sender.SenderOptions


@Configuration
@EnableKafka
class KafkaConfig {
    @Value("\${app.topics.stock.price}")
    private lateinit var stockPriceTopic: String
    @Value("\${app.topics.trade.trade-event}")
    private lateinit var tradeEventTopic: String
    @Value("\${app.topics.alert.volatility-alerts}")
    private lateinit var volatilityAlertsTopic: String


    @Bean
    fun reactiveKafkaProducerTemplate(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String
    ): ReactiveKafkaProducerTemplate<String, Any> {
        val props = hashMapOf<String, Any>(
            "bootstrap.servers" to bootstrapServers,
            "key.serializer" to StringSerializer::class.java,
            "value.serializer" to JsonSerializer::class.java,
            "spring.json.trusted.packages" to "*"
        )

        return ReactiveKafkaProducerTemplate(SenderOptions.create(props))
    }

    @Bean
    fun defaultKafkaStreamsConfig(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${spring.kafka.streams.application-id}") appId: String
    ): KafkaStreamsConfiguration {
        val props = mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to appId,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.StringSerde::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to JsonSerde::class.java,
            "spring.json.trusted.packages" to "*"
        )
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun tradeTopic(): NewTopic {
        return NewTopic(tradeEventTopic, 4, 1)
    }

    @Bean
    fun stockPriceTopic(): NewTopic {
        return NewTopic(stockPriceTopic, 4, 1)
    }

    @Bean
    fun volatilityAlertsTopic(): NewTopic {
        return NewTopic(volatilityAlertsTopic, 4, 1)
    }
}