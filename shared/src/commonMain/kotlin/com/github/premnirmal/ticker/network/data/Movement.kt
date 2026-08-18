package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.shared.CommonParcelable
import com.github.premnirmal.shared.CommonParcelize
import kotlinx.serialization.Serializable

/** Tolerance for float share comparisons (selling "everything" after fractional buys). */
const val SHARE_EPSILON = 1e-4f

enum class MovementType { BUY, SELL }

/**
 * One immutable entry in a symbol's buy/sell ledger. Order in the ledger is the persistence
 * order ([id] ascending) — movements carry no dates by design.
 */
@CommonParcelize
@Serializable
data class Movement(
    val symbol: String,
    val type: MovementType,
    val shares: Float,
    val price: Float,
    var id: Long? = null
) : CommonParcelable

/** A movement paired with its realized gain — null for BUY movements. */
data class MovementGain(val movement: Movement, val gain: Float?)

/** The state of a symbol's pool after replaying its full ledger. */
data class LedgerSummary(
    val shares: Float,
    val averagePrice: Float,
    val costBasis: Float,
    val realizedGain: Float,
    val movementGains: List<MovementGain>
)

/**
 * Replays the ledger in order with average-cost accounting: buys grow the pool and move the
 * average; sells remove shares at the current average, locking the difference in as realized
 * gain. Assumes a valid ledger (see [isValidLedger]).
 */
fun List<Movement>.replayLedger(): LedgerSummary {
    var shares = 0.0
    var costBasis = 0.0
    var realized = 0.0
    val gains = ArrayList<MovementGain>(size)
    for (movement in this) {
        when (movement.type) {
            MovementType.BUY -> {
                shares += movement.shares.toDouble()
                costBasis += movement.shares.toDouble() * movement.price.toDouble()
                gains.add(MovementGain(movement, null))
            }
            MovementType.SELL -> {
                val average = if (shares <= 0.0) 0.0 else costBasis / shares
                val gain = (movement.price.toDouble() - average) * movement.shares.toDouble()
                realized += gain
                shares -= movement.shares.toDouble()
                costBasis -= movement.shares.toDouble() * average
                if (shares < SHARE_EPSILON.toDouble()) {
                    shares = 0.0
                    costBasis = 0.0
                }
                gains.add(MovementGain(movement, gain.toFloat()))
            }
        }
    }
    val averagePrice = if (shares <= 0.0) 0.0 else costBasis / shares
    return LedgerSummary(
        shares.toFloat(),
        averagePrice.toFloat(),
        costBasis.toFloat(),
        realized.toFloat(),
        gains
    )
}

/** Whether no SELL in the ledger ever exceeds the shares held at that point. */
fun List<Movement>.isValidLedger(): Boolean {
    var shares = 0.0
    for (movement in this) {
        when (movement.type) {
            MovementType.BUY -> shares += movement.shares.toDouble()
            MovementType.SELL -> {
                if (movement.shares.toDouble() > shares + SHARE_EPSILON.toDouble()) return false
                shares -= movement.shares.toDouble()
            }
        }
    }
    return true
}

/**
 * Synthesizes the derived [Position] for this pool — a single holding at the average price —
 * so every existing consumer of Quote.position (widgets, totals, detail screen) keeps working.
 */
fun LedgerSummary.toPosition(symbol: String): Position =
    if (shares <= 0f) {
        Position(symbol)
    } else {
        Position(symbol, mutableListOf(Holding(symbol, shares, averagePrice)))
    }
