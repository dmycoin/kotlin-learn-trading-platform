package io.trading.trading_platform.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal

data class WalletDto (
    val traderId: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val balance: BigDecimal,
)
