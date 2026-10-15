package com.douduoyu.app

internal class CellIndexMask private constructor(
    private val words: LongArray,
    override val size: Int,
) : AbstractSet<Int>() {
    override fun contains(element: Int): Boolean {
        if (element < 0) return false
        val wordIndex = element ushr WORD_SHIFT
        if (wordIndex >= words.size) return false
        return words[wordIndex] and bitFor(element) != 0L
    }

    override fun iterator(): Iterator<Int> =
        object : Iterator<Int> {
            private var wordIndex = 0
            private var remainingBits = words.firstOrNull() ?: 0L
            private var nextIndex = findNext()

            override fun hasNext(): Boolean = nextIndex >= 0

            override fun next(): Int {
                if (nextIndex < 0) throw NoSuchElementException()
                val result = nextIndex
                nextIndex = findNext()
                return result
            }

            private fun findNext(): Int {
                while (wordIndex < words.size) {
                    if (remainingBits != 0L) {
                        val bitIndex = java.lang.Long.numberOfTrailingZeros(remainingBits)
                        remainingBits = remainingBits and (remainingBits - 1L)
                        return (wordIndex shl WORD_SHIFT) + bitIndex
                    }
                    wordIndex += 1
                    remainingBits = words.getOrElse(wordIndex) { 0L }
                }
                return -1
            }
        }

    fun containsAll(other: CellIndexMask): Boolean {
        for (wordIndex in other.words.indices) {
            val required = other.words[wordIndex]
            if (words.getOrElse(wordIndex) { 0L } and required != required) return false
        }
        return true
    }

    fun withIndices(indices: Iterable<Int>): CellIndexMask = updated(indices, include = true)

    fun withoutIndices(indices: Iterable<Int>): CellIndexMask = updated(indices, include = false)

    fun withIndices(indices: CellIndexMask): CellIndexMask {
        if (indices.isEmpty()) return this
        val nextWords = words.copyOf(maxOf(words.size, indices.words.size))
        var nextSize = size
        var changed = false
        for (wordIndex in indices.words.indices) {
            val addedBits = indices.words[wordIndex] and nextWords[wordIndex].inv()
            if (addedBits != 0L) {
                nextWords[wordIndex] = nextWords[wordIndex] or addedBits
                nextSize += java.lang.Long.bitCount(addedBits)
                changed = true
            }
        }
        return if (changed) create(nextWords, nextSize) else this
    }

    fun withoutIndices(indices: CellIndexMask): CellIndexMask {
        if (isEmpty() || indices.isEmpty()) return this
        val nextWords = words.copyOf()
        var nextSize = size
        var changed = false
        val sharedWordCount = minOf(nextWords.size, indices.words.size)
        for (wordIndex in 0 until sharedWordCount) {
            val removedBits = nextWords[wordIndex] and indices.words[wordIndex]
            if (removedBits != 0L) {
                nextWords[wordIndex] = nextWords[wordIndex] and removedBits.inv()
                nextSize -= java.lang.Long.bitCount(removedBits)
                changed = true
            }
        }
        return if (changed) create(nextWords, nextSize) else this
    }

    fun toByteArray(): ByteArray {
        if (isEmpty()) return ByteArray(0)
        val lastWord = words.last()
        val lastWordBitCount = Long.SIZE_BITS - java.lang.Long.numberOfLeadingZeros(lastWord)
        val byteCount = (words.lastIndex * Long.SIZE_BYTES) + (lastWordBitCount + 7) / 8
        return ByteArray(byteCount) { byteIndex ->
            val word = words[byteIndex / Long.SIZE_BYTES]
            (word ushr ((byteIndex % Long.SIZE_BYTES) * Byte.SIZE_BITS)).toByte()
        }
    }

    private fun updated(
        indices: Iterable<Int>,
        include: Boolean,
    ): CellIndexMask {
        var nextWords = words.copyOf()
        var nextSize = size
        var changed = false
        for (index in indices) {
            if (index < 0) continue
            val wordIndex = index ushr WORD_SHIFT
            if (include && wordIndex >= nextWords.size) {
                val expandedSize = maxOf(wordIndex + 1, maxOf(1, nextWords.size * 2))
                nextWords = nextWords.copyOf(expandedSize)
            } else if (wordIndex >= nextWords.size) {
                continue
            }
            val bit = bitFor(index)
            val contains = nextWords[wordIndex] and bit != 0L
            if (include && !contains) {
                nextWords[wordIndex] = nextWords[wordIndex] or bit
                nextSize += 1
                changed = true
            } else if (!include && contains) {
                nextWords[wordIndex] = nextWords[wordIndex] and bit.inv()
                nextSize -= 1
                changed = true
            }
        }
        if (!changed) return this
        return create(nextWords, nextSize)
    }

    internal class Builder {
        private var words = LongArray(0)
        private var size = 0

        fun add(index: Int) {
            if (index < 0) return
            val wordIndex = index ushr WORD_SHIFT
            if (wordIndex >= words.size) {
                val expandedSize = maxOf(wordIndex + 1, maxOf(1, words.size * 2))
                words = words.copyOf(expandedSize)
            }
            val bit = bitFor(index)
            if (words[wordIndex] and bit == 0L) {
                words[wordIndex] = words[wordIndex] or bit
                size += 1
            }
        }

        fun build(): CellIndexMask = create(words, size)
    }

    companion object {
        private const val WORD_SHIFT = 6
        private const val WORD_MASK = 63
        private val Empty = CellIndexMask(LongArray(0), 0)

        fun empty(): CellIndexMask = Empty

        fun builder(): Builder = Builder()

        fun from(indices: Iterable<Int>): CellIndexMask {
            val builder = builder()
            indices.forEach(builder::add)
            return builder.build()
        }

        fun from(indices: IntArray): CellIndexMask {
            val builder = builder()
            indices.forEach(builder::add)
            return builder.build()
        }

        fun fromByteArray(bytes: ByteArray): CellIndexMask {
            if (bytes.isEmpty()) return Empty
            val words = LongArray((bytes.size + Long.SIZE_BYTES - 1) / Long.SIZE_BYTES)
            bytes.forEachIndexed { byteIndex, value ->
                val wordIndex = byteIndex / Long.SIZE_BYTES
                val shift = (byteIndex % Long.SIZE_BYTES) * Byte.SIZE_BITS
                words[wordIndex] = words[wordIndex] or ((value.toLong() and 0xFFL) shl shift)
            }
            var size = 0
            words.forEach { word -> size += java.lang.Long.bitCount(word) }
            return create(words, size)
        }

        private fun create(
            words: LongArray,
            size: Int,
        ): CellIndexMask {
            if (size == 0) return Empty
            var usedWordCount = words.size
            while (usedWordCount > 0 && words[usedWordCount - 1] == 0L) {
                usedWordCount -= 1
            }
            return CellIndexMask(words.copyOf(usedWordCount), size)
        }

        private fun bitFor(index: Int): Long = 1L shl (index and WORD_MASK)
    }
}
