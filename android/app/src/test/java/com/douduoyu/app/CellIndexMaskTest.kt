package com.douduoyu.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CellIndexMaskTest {
    @Test
    fun builderCreatesSetCompatibleMaskInIndexOrder() {
        val builder = CellIndexMask.builder()
        listOf(130, 1, 64, 130, -1).forEach(builder::add)
        val mask = builder.build()

        assertEquals(3, mask.size)
        assertEquals(listOf(1, 64, 130), mask.toList())
        assertEquals(setOf(1, 64, 130), mask)
        assertTrue(130 in mask)
        assertFalse(129 in mask)
    }

    @Test
    fun updatesDoNotMutateOriginalMask() {
        val original = CellIndexMask.from(listOf(1, 65, 262_143))
        val added = original.withIndices(CellIndexMask.from(listOf(2, 65)))
        val removed = added.withoutIndices(CellIndexMask.from(listOf(1, 262_143)))

        assertEquals(setOf(1, 65, 262_143), original)
        assertEquals(setOf(1, 2, 65, 262_143), added)
        assertEquals(setOf(2, 65), removed)
    }

    @Test
    fun noOpUpdatesReuseImmutableInstance() {
        val mask = CellIndexMask.from(intArrayOf(2, 66))

        assertSame(mask, mask.withIndices(listOf(2, 66)))
        assertSame(mask, mask.withoutIndices(listOf(3, 67)))
    }

    @Test
    fun maskContainmentChecksWholeBitset() {
        val mask = CellIndexMask.from(listOf(1, 63, 64, 127, 262_143))

        assertTrue(mask.containsAll(CellIndexMask.from(listOf(63, 127, 262_143))))
        assertFalse(mask.containsAll(CellIndexMask.from(listOf(63, 128))))
        assertTrue(mask.containsAll(CellIndexMask.empty()))
    }

    @Test
    fun dense512GridUsesMaskOperationsAtUpperBoundary() {
        val builder = CellIndexMask.builder()
        for (index in 0 until 512 * 512) builder.add(index)
        val dense = builder.build()

        assertEquals(262_144, dense.size)
        assertTrue(0 in dense)
        assertTrue(262_143 in dense)
        assertFalse(262_144 in dense)
        val encoded = dense.toByteArray()
        assertEquals(32_768, encoded.size)
        assertEquals(dense, CellIndexMask.fromByteArray(encoded))
        assertEquals(dense, CellIndexMask.empty().withIndices(dense))
        assertSame(CellIndexMask.empty(), dense.withoutIndices(dense))
    }

    @Test
    fun byteSerializationRoundTripsSparseAndEmptyMasks() {
        val sparse = CellIndexMask.from(listOf(0, 7, 8, 63, 64, 130))

        assertEquals(sparse, CellIndexMask.fromByteArray(sparse.toByteArray()))
        assertArrayEquals(
            byteArrayOf(0x81.toByte(), 0x81.toByte()),
            CellIndexMask.from(listOf(0, 7, 8, 15)).toByteArray(),
        )
        assertTrue(CellIndexMask.empty().toByteArray().isEmpty())
        assertSame(CellIndexMask.empty(), CellIndexMask.fromByteArray(byteArrayOf()))
    }
}
