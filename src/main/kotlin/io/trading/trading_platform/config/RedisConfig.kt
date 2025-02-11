package io.trading.trading_platform.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean("reactiveRedisTemplate")
    fun reactiveJsonRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
        val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            //Это добавит в JSON поле @class с именем класса для того, чтобы при сериализации понимать в какой объект нужно сериализовать
            activateDefaultTyping(
                polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
                JsonTypeInfo.As.PROPERTY
            )
        }

        val serializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
        val context = RedisSerializationContext.newSerializationContext<String, Any>(StringRedisSerializer())
            .key(StringRedisSerializer())
            .value(serializer)
            .hashKey(StringRedisSerializer())
            .hashValue(serializer)
            .build()
        return ReactiveRedisTemplate(factory, context)
    }
}