package io.trading.trading_platform.service.impl

import io.trading.trading_platform.exception.EntityNotFoundException
import io.trading.trading_platform.model.mongo.Wallet
import io.trading.trading_platform.model.mongo.Wallet.Companion.BALANCE_FIELD
import io.trading.trading_platform.model.mongo.Wallet.Companion.ID_FIELD
import io.trading.trading_platform.repository.WalletRepository
import io.trading.trading_platform.service.WalletService
import org.bson.types.Decimal128
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class WalletServiceImpl (
    private val mongoTemplate: ReactiveMongoTemplate,
    private val walletRepository: WalletRepository
) : WalletService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun withdrawAmount(traderId: String, amount: BigDecimal): Mono<Wallet> {
        return mongoTemplate.findAndModify(
            Query(
                Criteria.where(ID_FIELD).`is`(traderId)
                    .and(BALANCE_FIELD).gte(amount)
            ),
            Update().inc(BALANCE_FIELD, Decimal128(amount.negate())),
            FindAndModifyOptions.options().returnNew(true),
            Wallet::class.java
        )
            .switchIfEmpty(Mono.error(IllegalArgumentException("Insufficient funds or wallet not found")))
            .doOnError { error ->
                if (error is IllegalArgumentException) {
                    logger.error("Insufficient funds: traderId=$traderId, required amount=$amount")
                } else {
                    logger.error("Unexpected error: ${error.message}", error)
                }
        }
    }

    override fun depositAmount(traderId: String, amount: BigDecimal): Mono<Wallet> {
        return mongoTemplate.findAndModify(
            Query(
                Criteria.where(ID_FIELD).`is`(traderId)
            ),
            Update().inc(BALANCE_FIELD, Decimal128(amount)),
            FindAndModifyOptions.options().returnNew(true),
            Wallet::class.java
        )
            .switchIfEmpty(Mono.error(IllegalArgumentException("Wallet not found")))
            .doOnError { error ->
                if (error is IllegalArgumentException) {
                    logger.error("Wallet not found: traderId=$traderId")
                } else {
                    logger.error("Unexpected error: ${error.message}", error)
                }
            }
    }

    override fun getWallet(traderId: String): Mono<Wallet> {
        return walletRepository.findById(traderId).switchIfEmpty(Mono.error(EntityNotFoundException("Wallet not found with traderId $traderId")))
    }
}