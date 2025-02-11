package io.trading.trading_platform.repository

import io.trading.trading_platform.model.mongo.Wallet
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface WalletRepository : ReactiveMongoRepository<Wallet, String>