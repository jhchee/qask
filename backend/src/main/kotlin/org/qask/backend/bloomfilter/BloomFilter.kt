package org.qask.backend.bloomfilter

import hash.Murmur3
import java.util.*
import kotlin.math.ln

interface IBloomFilter {
    fun add(item: String)
    fun isPresent(item: String): Boolean
}

abstract class AbstractBloomFilter : IBloomFilter {
    protected lateinit var bitSet: BitSet

    constructor(positivePercentage: Double, expectedElement: Int) {
        // we can deduce out the size of bit set using this formula
        // m = -n * lnP / (ln2)^2
        val size = (-1 * (expectedElement * ln(positivePercentage)) / (ln(2.0) * ln(2.0))).ceilAndInt()
        val fnCount = ((size / expectedElement) * ln(2.0))
        bitSet = BitSet(size)
    }

    override fun add(item: String) {
        getHashedIndexForBitSet(item).forEach(bitSet::set)
    }

    override fun isPresent(item: String): Boolean {
        return getHashedIndexForBitSet(item).all(bitSet::get)
    }

    abstract fun getHashedIndexForBitSet(data: String): List<Int>

    fun Double.ceilAndInt(): Int {
        return Math.ceil(this).toInt()
    }
}

// implement our bloom filter with hashcode and hash function
class BloomFilter(tolerancePercentage: Double, expectedElement: Int) :
    AbstractBloomFilter(tolerancePercentage, expectedElement) {

    override fun getHashedIndexForBitSet(data: String): List<Int> {
        return listOf(
            Math.abs(data.hashCode()) % bitSet.size(),
            Math.abs(Murmur3.hash32(data.toByteArray())) % bitSet.size()
        )
    }
}