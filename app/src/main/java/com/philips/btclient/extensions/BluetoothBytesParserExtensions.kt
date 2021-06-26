/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.extensions

import android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16
import com.philips.btclient.acom.Observation
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothBytesParser.FORMAT_FLOAT
import com.welie.blessed.BluetoothBytesParser.FORMAT_UINT32
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Return an Integer value of the specified type. This operation will NOT advance the internal offset to the next position.
 *
 * @param formatType The format type used to interpret the byte(s) value
 * @return An Integer object or null in case the byte array was not valid
 */
fun BluetoothBytesParser.peekIntValue(formatType: Int): Int? {
    val result = getIntValue(formatType, offset, byteOrder)
    return result
}

/**
 * Return an Integer value of the specified type. This operation will NOT advance the internal offset to the next position.
 *
 * @param formatType The format type used to interpret the byte(s) value
 * @return An Integer object or null in case the byte array was not valid
 */
fun BluetoothBytesParser.isNextAttributeType(): Boolean {
    val nextAttribte = peekIntValue(BluetoothBytesParser.FORMAT_UINT32)
    return nextAttribte?.let { MdcConstants.isTypeAttribute(nextAttribte) } ?: false
}

fun BluetoothBytesParser.atEnd(): Boolean {
    return this.offset >= this.value.size - 1
}

/**
 * Return the next ACOM Observation. This operation will automatically advance the internal offset to the next position after the
 * observation bytes. Note it is assumed that the offset is at the start of the observation bytes.
 *
 * @param formatType the format type used to interpret the byte(s) value
 * @return an Integer object or null in case the observation was not valid
 */
fun BluetoothBytesParser.getObservation(): Observation? {
    if (atEnd() || !isNextAttributeType() ) {
        return null
    }
    return Observation(this)
}

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
 * @return The DateTime read from the bytes. This will cause an exception if bytes run past end. Will return 0 epoch if unparsable
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

fun Long.millisAsLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}

/*
 * Read bytes and return the ByteArray of the length passed in.  This will increment the offset
 *
 * @return The DateTime read from the bytes. This will cause an exception if bytes run past end. Will return 0 epoch if unparsable
 */
fun BluetoothBytesParser.getByteArray(length: Int): ByteArray {
    val array = getValue().copyOfRange(offset, offset + length)
    offset += length
    return array
}