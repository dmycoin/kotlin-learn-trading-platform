package io.trading.trading_platform.service.socket

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.trading.trading_platform.dto.FinnhubTradeMessageDto
import io.trading.trading_platform.dto.kafka.StockPriceDto
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Instant

@Component
@ConditionalOnProperty(value = ["app.socket.enabled"], havingValue = "true")
class FinnhubWebSocketService (
    private val objectMapper: ObjectMapper,
    private val kafkaTemplate: KafkaTemplate<String, StockPriceDto>
) : TextWebSocketHandler() {
    private lateinit var webSocketSession: WebSocketSession
    private val webSocketClient: WebSocketClient = StandardWebSocketClient()

    @Value("\${app.topics.stock.price}")
    private lateinit var stockTopicName : String
    @Value("\${app.stock.token}")
    private lateinit var token : String
    @Value("\${app.stock.url}")
    private lateinit var url : String

    private val logger = LoggerFactory.getLogger(javaClass)

    private val symbolsToSubscribe = listOf("AAPL", "AMD", "NVDA", "PLTR", "GOOGL", "UBER", "BBAI")

    @PostConstruct
    fun connect() {
        logger.info("Установка соединения с finhub")
        val uri = "${url}=${token}"
        webSocketClient.execute(this, uri).get()
        logger.info("Соединение с finhub установлено")
    }

    @PreDestroy
    fun disconnect() {
        if (::webSocketSession.isInitialized && webSocketSession.isOpen) {
            unsubscribe()
            webSocketSession.close()
            logger.info("Соединение с finhub успешнно отключено")
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        this.webSocketSession = session
        logger.info("Попытка подписаться на рыночное обновление акций finhub")
        symbolsToSubscribe.forEach { symbol ->
            session.sendMessage(TextMessage("""{"type":"subscribe","symbol":"$symbol"}"""))
        }
        logger.info("Подписка на рыночное обновление акиций успешно оформлена")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.info("Received message: ${message.payload}")
        try {
            val readValue = objectMapper.readValue(message.payload, FinnhubTradeMessageDto::class.java)

            readValue.data.groupBy { it.s }.forEach { (symbol, trades) ->
                val stockPrice = StockPriceDto(
                    symbol = symbol,
                    price = trades.last().p.toBigDecimal(),
                    timestamp = Instant.ofEpochMilli(trades.last().t)
                )

                logger.info("Sending message to Kafka: $stockPrice")

                kafkaTemplate.send(stockTopicName, stockPrice.symbol, stockPrice)
            }
        } catch (e: JsonProcessingException) {
            //TODO заглушка на время закрытия рынка
            logger.debug("Рынок закрыт")
        }
    }

    fun unsubscribe() {
        if (::webSocketSession.isInitialized && webSocketSession.isOpen) {
            symbolsToSubscribe.forEach { symbol ->
                webSocketSession.sendMessage(TextMessage("""{"type":"unsubscribe","symbol":"$symbol"}"""))
            }
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        println("WebSocket error: ${exception.message}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("WebSocket connection closed: ${status.reason}")
    }
}