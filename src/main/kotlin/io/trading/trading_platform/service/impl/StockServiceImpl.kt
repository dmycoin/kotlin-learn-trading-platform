package io.trading.trading_platform.service.impl

import io.trading.trading_platform.dto.kafka.StockPriceDto
import io.trading.trading_platform.model.mongo.StockPrice
import io.trading.trading_platform.repository.StockPriceRepository
import io.trading.trading_platform.service.StockService
import io.trading.trading_platform.util.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class StockServiceImpl (
    private val stockRepository: StockPriceRepository,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>
) : StockService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun updateStockPrice(stockPriceDto: StockPriceDto): Mono<StockPrice> {
        return stockRepository.findBySymbol(stockPriceDto.symbol)
            .map {
                it.copy(price = stockPriceDto.price, timestamp = stockPriceDto.timestamp)
            }
            .flatMap {
                stockRepository.save(it)
            }
            .switchIfEmpty(
                stockRepository.save(StockPrice(stockPriceDto.symbol, stockPriceDto.price, stockPriceDto.timestamp))
            )
            .flatMap { stock ->
                reactiveRedisTemplate.opsForValue().set(RedisKeys.stockKey(stock.symbol), stockPriceDto).thenReturn(stock)
            }
            .doOnSuccess{
                logger.debug("Operation completed: {} (price: {})", it.symbol, it.price)
            }
            .doOnError {
                logger.error("Failed to process stock: $stockPriceDto", it)
            }
    }

    override fun getStockPrice(symbol: String): Mono<StockPrice> {
        return reactiveRedisTemplate.opsForValue().get(RedisKeys.stockKey(symbol))
            .map { it as StockPriceDto }
            .flatMap { stock ->
                Mono.just(StockPrice(symbol, stock.price, stock.timestamp))
            }
            .switchIfEmpty(stockRepository.findBySymbol(symbol))
    }
}