package io.trading.trading_platform.config

import org.bson.types.Decimal128
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.math.BigDecimal

@Configuration
@EnableReactiveMongoRepositories(basePackages = ["io.trading.trading_platform.repository"])
class MongoConfig {

    @Bean
    fun transactionManager(dbFactory: ReactiveMongoDatabaseFactory): ReactiveMongoTransactionManager {
        return ReactiveMongoTransactionManager(dbFactory)
    }

    @Bean
    fun mongoCustomConversions() : MongoCustomConversions {
        return MongoCustomConversions(listOf(
            BigDecimalToDecimal128Converter(),
            Decimal128ToBigDecimalConverter()
        ));
    }
}

// Конвертер из BigDecimal в Decimal128
class BigDecimalToDecimal128Converter : org.springframework.core.convert.converter.Converter<BigDecimal, Decimal128> {
    override fun convert(source: BigDecimal): Decimal128 = Decimal128(source)
}

// Конвертер из Decimal128 в BigDecimal
class Decimal128ToBigDecimalConverter : org.springframework.core.convert.converter.Converter<Decimal128, BigDecimal> {
    override fun convert(source: Decimal128): BigDecimal = source.bigDecimalValue()
}