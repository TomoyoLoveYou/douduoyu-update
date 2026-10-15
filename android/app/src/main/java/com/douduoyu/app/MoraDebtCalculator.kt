package com.douduoyu.app

import java.math.BigDecimal
import java.math.MathContext

internal data class MoraDebtSnapshot(
    val nowEpochMillis: Long,
    val hasStarted: Boolean,
    val completedDays: Long,
    val currentAmount: BigDecimal,
    val accruedInterest: BigDecimal,
    /** Interest for the current compound day, applied at the next daily boundary. */
    val todayInterest: BigDecimal,
    val millisUntilNextCompound: Long,
) {
    /** Backward-compatible name used by older callers. */
    val nextInterest: BigDecimal
        get() = todayInterest
}

internal data class MoraDebtPoint(
    val epochMillis: Long,
    val completedDays: Long,
    val amount: BigDecimal,
)

internal object MoraDebtCalculator {
    const val START_EPOCH_MILLIS = 1_783_612_800_000L
    const val DAY_MILLIS = 86_400_000L
    val initialAmount: BigDecimal = BigDecimal("30000000")
    private val dailyInterestRate = BigDecimal("0.01")
    private val dailyFactor = BigDecimal.ONE.add(dailyInterestRate)
    private val mathContext = MathContext.DECIMAL128

    fun snapshot(nowEpochMillis: Long): MoraDebtSnapshot {
        val hasStarted = nowEpochMillis >= START_EPOCH_MILLIS
        val elapsedMillis = if (hasStarted) nowEpochMillis - START_EPOCH_MILLIS else 0L
        val completedDays = elapsedMillis / DAY_MILLIS
        val currentAmount = amountAfterDays(completedDays)
        val millisUntilNextCompound =
            if (hasStarted) {
                val remainder = elapsedMillis % DAY_MILLIS
                if (remainder == 0L) DAY_MILLIS else DAY_MILLIS - remainder
            } else {
                positiveDifference(START_EPOCH_MILLIS, nowEpochMillis)
            }

        return MoraDebtSnapshot(
            nowEpochMillis = nowEpochMillis,
            hasStarted = hasStarted,
            completedDays = completedDays,
            currentAmount = currentAmount,
            accruedInterest = currentAmount.subtract(initialAmount, mathContext),
            // Keep today's interest as an explicit 1% of the current balance. Computing it
            // from two separately rounded compounded amounts loses precision over long runs.
            todayInterest = currentAmount.multiply(dailyInterestRate, mathContext),
            millisUntilNextCompound = millisUntilNextCompound,
        )
    }

    fun amountAfterDays(days: Long): BigDecimal {
        require(days >= 0L)
        var exponent = days
        var base = dailyFactor
        var result = BigDecimal.ONE
        while (exponent > 0L) {
            if ((exponent and 1L) != 0L) {
                result = result.multiply(base, mathContext)
            }
            exponent = exponent ushr 1
            if (exponent > 0L) {
                base = base.multiply(base, mathContext)
            }
        }
        return initialAmount.multiply(result, mathContext)
    }

    fun projection(
        endDay: Long,
        maxPoints: Int = 61,
    ): List<MoraDebtPoint> {
        require(endDay >= 0L)
        require(maxPoints >= 2)
        if (endDay == 0L) {
            return listOf(pointAtDay(0L))
        }

        val pointCount = minOf(endDay + 1L, maxPoints.toLong()).toInt()
        val intervals = pointCount - 1
        return List(pointCount) { index ->
            pointAtDay(proportionalDay(index = index, intervals = intervals, totalDays = endDay))
        }
    }

    private fun pointAtDay(day: Long): MoraDebtPoint {
        return MoraDebtPoint(
            epochMillis = START_EPOCH_MILLIS + day * DAY_MILLIS,
            completedDays = day,
            amount = amountAfterDays(day),
        )
    }

    private fun proportionalDay(
        index: Int,
        intervals: Int,
        totalDays: Long,
    ): Long {
        val indexLong = index.toLong()
        val intervalsLong = intervals.toLong()
        return (totalDays / intervalsLong) * indexLong + ((totalDays % intervalsLong) * indexLong) / intervalsLong
    }

    private fun positiveDifference(
        later: Long,
        earlier: Long,
    ): Long {
        return try {
            Math.subtractExact(later, earlier)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
    }
}
