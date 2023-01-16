package com.philips.bleclient.extensions

/*
 * Support for Flags in Kotlin (may want to move to a Flags.kt file
 */
open class BitMask(val value: Long)

interface Flags  {
    val bit: Long

    fun toBitMask(): BitMask = BitMask(bit)
}

infix fun Flags.and(other: Long): BitMask = BitMask(bit and other)
infix fun <T: Flags> Flags.or(other: T): BitMask = BitMask(bit or other.bit)

operator infix fun Flags.plus(other: Flags): BitMask = BitMask(bit or other.bit)

inline fun <reified T> enabledValues(mask: BitMask) : List<T> where T : Enum<T>, T : Flags {
    return enumValues<T>().filter { mask hasFlag it }
}


infix fun <T: Flags> BitMask.unset(which: T): BitMask = BitMask(value xor which.bit)
infix fun <T: Flags> BitMask.set(which: T): BitMask = BitMask(value or which.bit)

infix fun BitMask.or(other: Flags): BitMask = BitMask(value or other.bit)

operator infix fun BitMask.plus(other: BitMask): BitMask = BitMask(value or other.value)
operator infix fun BitMask.plus(other: Flags): BitMask = BitMask(value or other.bit)

infix fun <T: Flags> BitMask.hasFlag(which: T): Boolean {
    // an Undefined flag is a special case.
    if(value == 0L || (value > 0L && which.bit == 0L)) return false

    return value and which.bit == which.bit
}

// End Flags support stuff
