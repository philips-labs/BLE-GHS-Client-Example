/*
 * Copyright (c) Koninklijke Philips N.V. 2023.
 * All rights reserved.
 */
package com.philips.bleclient.extensions

/*
 * Support for Flags in Kotlin
 */
open class BitMask(val value: Long) {
    constructor(intValue: Int) : this(intValue.toLong())
    constructor(byteValue: Byte) : this(byteValue.toLong())
}

interface Flags  {
    val bits: Long
    fun toBitMask(): BitMask = BitMask(bits)
}

infix fun Flags.and(other: Long): BitMask = BitMask(bits and other)
infix fun <T: Flags> Flags.or(other: T): BitMask = BitMask(bits or other.bits)

operator infix fun Flags.plus(other: Flags): BitMask = BitMask(bits or other.bits)

inline fun <reified T> enabledValues(mask: BitMask) : List<T> where T : Enum<T>, T : Flags = enumValues<T>().filter { mask hasFlag it }

infix fun <T: Flags> BitMask.set(which: T): BitMask = BitMask(value or which.bits)
infix fun <T: Flags> BitMask.unset(which: T): BitMask = BitMask(value xor which.bits)

infix fun BitMask.or(other: Flags): BitMask = BitMask(value or other.bits)

operator infix fun BitMask.plus(other: BitMask): BitMask = BitMask(value or other.value)
operator infix fun BitMask.plus(other: Flags): BitMask = BitMask(value or other.bits)

infix fun <T: Flags> BitMask.hasFlag(which: T): Boolean {
    // an undefined flag is a special case that is checked
    return if(value == 0L || (value > 0L && which.bits == 0L)) false else value and which.bits == which.bits
}

// End Flags support
