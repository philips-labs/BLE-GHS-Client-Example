/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.ghs

data class GenericHealthSensorMessageBytes(
    var receivedMessageBytes : ByteArray = ByteArray(0),
    var lastSequenceNumber : Byte  = 0xFF.toByte()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenericHealthSensorMessageBytes

        if (!receivedMessageBytes.contentEquals(other.receivedMessageBytes)) return false
        if (lastSequenceNumber != other.lastSequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receivedMessageBytes.contentHashCode()
        result = 31 * result + lastSequenceNumber
        return result
    }
}