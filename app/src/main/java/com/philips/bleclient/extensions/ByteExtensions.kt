/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
@file:Suppress("unused")

package com.philips.bleclient

import java.util.*

fun Byte.asHexString(): String {
    var hexString = this.toUINT8().toString(16).uppercase(Locale.ROOT)
    if (this.toUINT8() < 16) hexString = "0$hexString"
    return hexString
}

fun ByteArray.formatHexBytes(seperator: String?): String {
    var resultString = ""
    for ((index, value) in this.iterator().withIndex()) {
        resultString += value.asHexString()
        if (seperator != null && index < (this.size - 1)) resultString += seperator
    }
    return resultString
}

fun ByteArray.asHexString(): String {
    return this.formatHexBytes(null)
}

fun ByteArray.asFormattedHexString(): String {
    return this.formatHexBytes(" ")
    // return asHexString().replace("..".toRegex(), "$0 ")
}

fun ByteArray.asAsciiString(): String {
    var resultString = ""
    forEach {
        resultString += it.toInt().toChar()
    }
    return resultString
}

fun List<Byte>.formatHexBytes(seperator: String?): String {
    var resultString = ""
    for ((index, value) in iterator().withIndex()) {
        resultString += value.asHexString()
        if (seperator != null && index < (this.size - 1)) resultString += seperator
    }
    return resultString
}

fun Byte.toUINT8(): Int {
    return this.toInt() and 0xFF
}

/*
 * Merge the ByteArrays in the receiver into the returned ByteArray
 * This could be done with a fold function, but the concat of each cause a lot of allocs
 * So instead the method creates a large result ByteArray and copies each into it.
 * The "optimized" Kotlin implementation is kept commented out for comparison and
 * used fold instead of reduce so that empty list doesn't cause an exception
 */
fun List<ByteArray>.merge(): ByteArray {
    return this.fold(byteArrayOf(), { result, bytes -> result + bytes })
}
