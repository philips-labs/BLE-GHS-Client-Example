/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
@file:Suppress("unused")

package com.philips.bleclient.extensions

import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothBytesParser.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import java.nio.ByteOrder

fun BluetoothBytesParser.atEnd(): Boolean {
    return this.offset >= this.value.size - 1
}

/**
 * Return a 11073-20601 (MDER) float value of the specified format and byte order. This operation will automatically advance the internal offset to the next position.
 *
 * @return The float value at the position of the internal offset
 */
fun BluetoothBytesParser.getMderFloatValue(): Float {
    return getMderFloatValue(offset)
}

const val MDER_POSITIVE_INFINITY = 0x007FFFFE
const val MDER_FIRST_RESERVED_VALUE = MDER_POSITIVE_INFINITY
const val MDER_NaN = 0x007FFFFF
const val MDER_NRes = 0x00800000
const val MDER_RESERVED_VALUE = 0x00800001
const val MDER_NEGATIVE_INFINITY = 0x00800002
val MDER_RESERVED_FLOAT_VALUES = floatArrayOf( Float.POSITIVE_INFINITY, Float.NaN, Float.NaN, Float.NaN, Float.NEGATIVE_INFINITY)

/**
 * Returns the size of a give value type.
 * This is copied from BluetoothBytesParser in Blessed because it's private there
 * and it is used in BluetoothBytesParser.getMderFloatValue
 */
private fun BluetoothBytesParser.getTypeLen(formatType: Int): Int {
    return formatType and 0xF
}

/**
 * Return a 11073-20601 (MDER) float value of the specified format, offset and byte order. This operation will advance the internal offset to the next position.
 *
 * @return The float value at the position of the internal offset
 */
fun BluetoothBytesParser.getMderFloatValue(index: Int): Float {
    val result = getFloatValue(FORMAT_FLOAT, index, this.byteOrder)
    offset += getTypeLen(FORMAT_FLOAT)
    return result
}

fun BluetoothBytesParser.getGHSDateTimeFlags(): BitMask {
    return BitMask(getIntValue(FORMAT_UINT8).toLong())
}

/*
 * Read bytes (as a GHS date time) and return the DateTime object representing the value. This will increment the offset
 * past the time bytes struct (depending on if time counter or world time 6-10
 * The GHS time stamp is currently defined with the following structure:
 *
 * Flags (uint8, Mandatory) - flag bits (described below)
 *
 * Time counter (uint64, Present for time counter flag) - millisecond tick counter
 *
 * Date-time (uint32, Present for world time flag) - seconds since 1/1/2000
 * Milliseconds (uint16, Present for world time flag) - milliseconds (combined with Date-time for msec since Y2K epoch)
 * Time set/Sync Source (uint8, Present for world time flag) - flags indicating the source and time sync methods (described below)
 * TZ Offset (uint8, Present for world time flag) - Timezone offset
 * DST Offset (uint8, Present for world time flag) - Daylight savings time offset
 *
 * @return The DateTime read from the bytes. This will cause an exception if bytes run past end. Will return null if the flags indicate a tick counter
 */

fun BluetoothBytesParser.getGHSDateTime(timeFlags: BitMask): LocalDateTime? {

    val isUTC = timeFlags hasFlag TimestampFlags.isUTC
    val isTicks = timeFlags hasFlag TimestampFlags.isTickCounter
    val hasMillis = timeFlags hasFlag TimestampFlags.isMilliseconds
    val hasTZ = timeFlags hasFlag TimestampFlags.isTZPresent
    val isCurrentTimeline = timeFlags hasFlag TimestampFlags.isCurrentTimeline

    // Double check if the time is actually a time and not ticks according to flags
    var result: LocalDateTime? = null
    if (!isTicks) {
        val timeValue = getGHSLongValue(ByteOrder.LITTLE_ENDIAN)
        val unixEpochMillis = (timeValue * if (hasMillis) 1L else 1000L) + UTC_TO_UNIX_EPOCH_MILLIS
        Timber.i("Parsed GHS UTC epoch millis: $unixEpochMillis")
        result = if (isUTC) {
            (unixEpochMillis).millisAsLocalDateTime()
        } else {
            val timeSourceMethod = getIntValue(FORMAT_UINT8)
            val utcOffset = if (hasTZ) getIntValue(FORMAT_SINT8).toLong() * MILLIS_IN_15_MINUTES else 0L
            // creating a local date time so don't need to use the utcOffset and build it on just the UTC milliseconds.
            // If creating a full timestamp that was local to the observation we'd need the utc offset for DST and TZ as seperate components
            Timber.i("Parsed GHS TZ/DST offset millis: $utcOffset")
            (unixEpochMillis).millisAsLocalDateTime()
        }
    }
    Timber.i("Parsed GHS Date: $result")
    return result
}

/*
 * Return the 6 byte "long" time counter used in GHS
 */
fun BluetoothBytesParser.getGHSTimeCounter(): Long {
    /*
    val bytes = getByteArray(6)
    var value = (0x00FF and bytes[5].toInt()).toLong()
    for (i in 5 downTo 0) {
        value = value shl 8
        value += (0x00FF and bytes[i].toInt()).toLong()
    }
    return value
     */
    return getLongValue(FORMAT_UINT48)
}

fun Long.millisAsLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}


fun Long.millisAsUTCDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC)
}

fun BluetoothBytesParser.getGHSLongValue(): Long {
    return getLongValue(FORMAT_UINT64)
}

/**
 * Return a Long value using the specified byte order. This operation will automatically advance the internal offset to the next position.
 *
 * @return an Long object or null in case the byte array was not valid
 */
fun BluetoothBytesParser.getGHSLongValue(byteOrder: ByteOrder): Long {
    val result = getLongValue(offset, byteOrder)
    offset += 8
    return result
}

/**
 * Return a Long value using the specified byte order and offset position. This operation will not advance the internal offset to the next position.
 *
 * @return an Long object or null in case the byte array was not valid
 */
fun BluetoothBytesParser.getGHSLongValue(offset: Int, byteOrder: ByteOrder): Long {
    return if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
        var value = getByteAsLongAt(offset + 7)
        for (i in 6 downTo 0) {
            value = value shl 8
            value += getByteAsLongAt(offset + i)
        }
        value
    } else {
        var value = getByteAsLongAt(offset)
        for (i in 1..7) {
            value = value shl 8
            value += getByteAsLongAt(offset + i)
        }
        value
    }
}

fun BluetoothBytesParser.getByteAsLongAt(offset: Int): Long {
    return (0x00FF and getValue().get(offset + 7).toInt()).toLong()
}