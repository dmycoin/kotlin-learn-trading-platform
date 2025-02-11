package io.trading.trading_platform.service

import io.trading.trading_platform.model.mongo.Wallet
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface WalletService {
    fun withdrawAmount(traderId: String, amount: BigDecimal): Mono<Wallet>
    fun depositAmount(traderId: String, amount: BigDecimal): Mono<Wallet>
    fun getWallet(traderId: String): Mono<Wallet>
}