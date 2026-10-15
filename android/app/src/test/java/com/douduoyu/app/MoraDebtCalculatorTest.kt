package com.douduoyu.app

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoraDebtCalculatorTest {
    @Test
    fun snapshot_compoundsOnlyAfterEachFullDay() {
        assertAmount("30000000", MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS - 1L).currentAmount)
        assertAmount("30000000", MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS).currentAmount)
        assertAmount(
            "30000000",
            MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS + MoraDebtCalculator.DAY_MILLIS - 1L).currentAmount,
        )
        assertAmount(
            "30300000",
            MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS + MoraDebtCalculator.DAY_MILLIS).currentAmount,
        )
        assertAmount(
            "30603000",
            MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS + MoraDebtCalculator.DAY_MILLIS * 2L).currentAmount,
        )
    }

    @Test
    fun snapshot_reportsTodayInterestAsOnePercentOfCurrentBalance() {
        val atStart = MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS)
        assertAmount("300000", atStart.todayInterest)
        // Keep the old property equivalent while callers migrate to the explicit name.
        assertAmount("300000", atStart.nextInterest)

        val afterOneYear = MoraDebtCalculator.snapshot(
            MoraDebtCalculator.START_EPOCH_MILLIS + MoraDebtCalculator.DAY_MILLIS * 365L,
        )
        assertAmount("1133503029.986614766328498143894929", afterOneYear.currentAmount)
        assertAmount("11335030.29986614766328498143894929", afterOneYear.todayInterest)
    }

    @Test
    fun snapshot_beforeStartKeepsPrincipalAndCountsDownToStart() {
        val snapshot = MoraDebtCalculator.snapshot(MoraDebtCalculator.START_EPOCH_MILLIS - 12_345L)

        assertFalse(snapshot.hasStarted)
        assertEquals(0L, snapshot.completedDays)
        assertEquals(12_345L, snapshot.millisUntilNextCompound)
        assertAmount("30000000", snapshot.currentAmount)
    }

    @Test
    fun projection_isBoundedOrderedAndMonotonic() {
        val points = MoraDebtCalculator.projection(endDay = 365L, maxPoints = 61)

        assertEquals(61, points.size)
        assertEquals(0L, points.first().completedDays)
        assertEquals(365L, points.last().completedDays)
        assertTrue(points.zipWithNext().all { (left, right) -> left.epochMillis < right.epochMillis })
        assertTrue(points.zipWithNext().all { (left, right) -> left.amount <= right.amount })
    }

    @Test
    fun amountAfterDays_hasStableLongTermDecimal128Values() {
        assertAmount("1133503029.986614766328498143894929", MoraDebtCalculator.amountAfterDays(365L))
        assertAmount("42827637299.62788313841228024629989", MoraDebtCalculator.amountAfterDays(730L))
    }

    @Test
    fun projection_samplesOneAndTwoYearRangesWithoutDrift() {
        val oneYearDays = listOf(
            0L, 6L, 12L, 18L, 24L, 30L, 36L, 42L, 48L, 54L, 60L, 66L, 73L, 79L, 85L, 91L,
            97L, 103L, 109L, 115L, 121L, 127L, 133L, 139L, 146L, 152L, 158L, 164L, 170L,
            176L, 182L, 188L, 194L, 200L, 206L, 212L, 219L, 225L, 231L, 237L, 243L, 249L,
            255L, 261L, 267L, 273L, 279L, 285L, 292L, 298L, 304L, 310L, 316L, 322L, 328L,
            334L, 340L, 346L, 352L, 358L, 365L,
        )
        val twoYearDays = listOf(
            0L, 12L, 24L, 36L, 48L, 60L, 73L, 85L, 97L, 109L, 121L, 133L, 146L, 158L, 170L,
            182L, 194L, 206L, 219L, 231L, 243L, 255L, 267L, 279L, 292L, 304L, 316L, 328L,
            340L, 352L, 365L, 377L, 389L, 401L, 413L, 425L, 438L, 450L, 462L, 474L, 486L,
            498L, 511L, 523L, 535L, 547L, 559L, 571L, 584L, 596L, 608L, 620L, 632L, 644L,
            657L, 669L, 681L, 693L, 705L, 717L, 730L,
        )

        assertProjectionSamples(365L, oneYearDays)
        assertProjectionSamples(730L, twoYearDays)
    }

    private fun assertProjectionSamples(
        endDay: Long,
        expectedDays: List<Long>,
    ) {
        val points = MoraDebtCalculator.projection(endDay = endDay, maxPoints = 61)
        assertEquals(expectedDays, points.map { it.completedDays })
        points.forEach { point ->
            assertEquals(MoraDebtCalculator.START_EPOCH_MILLIS + point.completedDays * MoraDebtCalculator.DAY_MILLIS, point.epochMillis)
            assertAmount(MoraDebtCalculator.amountAfterDays(point.completedDays).toPlainString(), point.amount)
        }
    }

    private fun assertAmount(
        expected: String,
        actual: BigDecimal,
    ) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
