/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
@file:Suppress("unused")

package com.philips.bleclient.extensions

import android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16
import com.philips.bleclient.acom.Observation
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothBytesParser.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.nio.ByteOrder

/**
 * Return an Integer value of the specified type. This operation will NOT advance the internal offset to the next position.
 *
 * @param formatType The format type used to interpret the byte(s) value
 * @return An Integer object or null in case the byte array was not valid
 */
fun BluetoothBytesParser.peekIntValue(formatType: Int): Int? {
    return getIntValue(formatType, offset, byteOrder)
}

/**
 * Test if the next attribute (32-bit int) is a ACOM object type attribute (used in receiving bytes
 * to indicate that the last object has completed as attributes can be in any order)
 * This will not change the offset into the bytes being parsed
 *
 * @return True is the next attribute is a object type attribute
 */
fun BluetoothBytesParser.isNextAttributeType(): Boolean {
    val nextAttribute = peekIntValue(FORMAT_UINT32)
    return nextAttribute?.let { MdcConstants.isTypeAttribute(nextAttribute) } ?: false
}

fun BluetoothBytesParser.atEnd(): Boolean {
    return this.offset >= this.value.size - 1
}

/**
 * Return the next ACOM Observation. This operation will automatically advance the internal offset to the next position after the
 * observation bytes. Note it is assumed that the offset is at the start of the observation bytes.
 *
 * @return the next observation from the bytes received. Return null if the next bytes are not an observation
 */
fun BluetoothBytesParser.getObservation(): Observation? {
    if (atEnd() || !isNextAttributeType() ) {
        return null
    }
    return Observation(this)
}

/**
 * Return all the next ACOM Observations. This operation will read observations until no more can be read from the bytes.
 *
 * @return the a list observations from the bytes received (could be empty if no observations)
 */
fun BluetoothBytesParser.getObservations(): List<Observation> {
    val observations = mutableListOf<Observation>()
    var obs: Observation? = getObservation()
    while( obs != null) {
        observations.add(obs)
        obs = getObservation()
    }
    return observations
}

/**
 * Return a 11073-20601 (MDER) float value of the specified format and byte order. This operation will automatically advance the internal offset to the next position.
 *
 * @return The float value at the position of the internal offset
 */
fun BluetoothBytesParser.getMderFloatValue(): Float? {
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
fun BluetoothBytesParser.getMderFloatValue(index: Int): Float? {
    val result = getFloatValue(FORMAT_FLOAT, index, this.byteOrder)
    offset += getTypeLen(FORMAT_FLOAT)
    return result
}

/*
 * Read bytes (as a Acom date time) and return the DateTime object representing the value. This will increment the offset
 * Length determines how to read the epoch second value and lengths can be:
 *
 * 4 bytes - value is a UInt32 representing seconds since Unix epoch
 * 6 bytes - value is a UInt32 representing milliseconds since Unix epoch (6 bytes is enough for the next few hundred years)
 * 8 bytes - value is a UInt32 representing milliseconds since Unix epoch
 *
 * @return The DateTime read from the bytes. This will cause an exception if bytes run past end. Will return 0 epoch if not parsable
 */
fun BluetoothBytesParser.getAcomDateTime(byteLength: Int): LocalDateTime {

    return when(byteLength) {
        4 -> (getIntValue(FORMAT_UINT32)?.toLong()!! * 1000L).millisAsLocalDateTime()
        6 -> {
            val topVal = getIntValue(FORMAT_UINT16)?.toLong()!!
            val bottomVal = getIntValue(FORMAT_UINT32)?.toLong()!!
            (topVal.shl(32) + bottomVal).millisAsLocalDateTime()
        }
        8 -> longValue.millisAsLocalDateTime()
        else -> 0L.millisAsLocalDateTime()
    }
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

private const val UTC_TO_UNIX_EPOCH_MILLIS = 946684800000L

fun BluetoothBytesParser.getGHSDateTime(timeFlags: BitMask): LocalDateTime? {

    val isUTC = timeFlags hasFlag GhsTimestampFlags.isUTC
    val isTicks = timeFlags hasFlag GhsTimestampFlags.isTickCounter
    val hasMillis = timeFlags hasFlag GhsTimestampFlags.isMillisecondsPresent
    val hasTZ = timeFlags hasFlag GhsTimestampFlags.isTZPresent
    val hasDST = timeFlags hasFlag GhsTimestampFlags.isDSTPresent
    val isCurrentTimeline = timeFlags hasFlag GhsTimestampFlags.isCurrentTimeline

    // Double check if the time is actually a time and not ticks according to flags
    return if (isTicks) {
        null
    } else {
        val unixEpochMillis = (getLongValue(ByteOrder.LITTLE_ENDIAN) * 1000L) + UTC_TO_UNIX_EPOCH_MILLIS
        val millis = if (hasMillis) getIntValue(FORMAT_UINT16).toLong() else 0L
        val timeSourceMethod = getIntValue(FORMAT_UINT8)
        val tzOffset = if (hasTZ) getIntValue(FORMAT_SINT8).toLong() else 0L
        val dstOffset = if (hasDST) getIntValue(FORMAT_SINT8).toLong() else 0L

        // TODO NOT LOCAL, USE TZ, DST OFFSET
        (unixEpochMillis + millis).millisAsLocalDateTime()
    }


//    return when(bits) {
//        4 -> (getIntValue(FORMAT_UINT32)?.toLong()!! * 1000L).millisAsLocalDateTime()
//        6 -> {
//            val topVal = getIntValue(FORMAT_UINT16)?.toLong()!!
//            val bottomVal = getIntValue(FORMAT_UINT32)?.toLong()!!
//            (topVal.shl(32) + bottomVal).millisAsLocalDateTime()
//        }
//        8 -> longValue.millisAsLocalDateTime()
//        else -> 0L.millisAsLocalDateTime()
//    }
//    return 0L.millisAsLocalDateTime()
}

fun BluetoothBytesParser.getGHSTimeCounter(): Long {
    return getLongValue(ByteOrder.LITTLE_ENDIAN)
}

fun Long.millisAsLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}
