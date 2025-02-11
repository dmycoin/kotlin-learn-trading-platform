package io.trading.trading_platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient

@Configuration
class WebSocketConfig {
    @Bean
    fun webSocketClient(): WebSocketClient = StandardWebSocketClient()
}