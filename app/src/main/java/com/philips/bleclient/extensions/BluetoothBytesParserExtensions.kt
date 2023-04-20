/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */

package com.philips.bleclient.extensions

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.observations.ObservationValue
import com.philips.bleclient.observations.TLVObservationValue
import com.philips.bleclient.toHexString
import com.welie.blessed.BluetoothBytesParser
import timber.log.Timber

/*
 * Return the 6 byte "long" time counter used in GHS
 */
fun BluetoothBytesParser.getGHSTimeCounter(): Long = uInt48

fun BluetoothBytesParser.tlvObservationValue(): TLVObservationValue {
    val numValues = getUInt8()
    val list = mutableListOf<Pair<Int, Any>>()
    repeat(numValues) {
        val type = getUInt32()
        val length = getUInt16()
        val formatType = getUInt8()
        val bytes = getByteArray(length)
        val value = getBluetoothValue(formatType, length)
        Timber.i("Parsed TLV type: ${type.toHexString()} length: $length format type: $formatType bytes: ${bytes.asFormattedHexString()} value: $value")
        list.add(Pair(type, value))
    }
    return TLVObservationValue(list)
}

fun BluetoothBytesParser.getBluetoothValue(formatType: Int, length: Int): Any {
    val parser = BluetoothBytesParser(getByteArray(length))
    return when (formatType) {
        4 -> parser.uInt8.toLong()
        6 -> parser.uInt16.toLong()
        7 -> parser.uInt24.toLong()
        8 -> parser.uInt32.toLong()
        9 -> parser.uInt48
        0xA -> parser.uInt64
        0xC -> parser.sInt8.toLong()
        0xE -> parser.sInt16.toLong()
        0xF -> parser.sInt24.toLong()
        0x10 -> parser.sInt32.toLong()
        0x19 -> parser.getStringValue()
        else -> -1.toLong()
    }
}
