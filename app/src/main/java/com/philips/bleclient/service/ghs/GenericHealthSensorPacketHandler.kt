package com.philips.bleclient.service.ghs

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.merge
import okhttp3.internal.and
import timber.log.Timber
import java.lang.RuntimeException

class GenericHealthSensorPacketHandler(val listener: GenericHealthSensorPacketListener) {

    private var packetBytesMap = mutableMapOf<String, ByteArray>()

    fun receiveBytes(deviceAddress: String, byteArray: ByteArray) {
        appendBLESegment(byteArray, deviceAddress)
        if (byteArray.isLastBLESegment()) {
            handlePacketsBytesComplete(deviceAddress)
        }
    }

    private fun appendBLESegment(byteArray: ByteArray, deviceAddress: String) {
        if (byteArray.isFirstBLESegment()) {
            packetBytesMap.put(deviceAddress, byteArray.withoutSegmentHeader())
        } else {
            val currentBytes = packetBytesMap.getOrDefault(deviceAddress, byteArrayOf())
            packetBytesMap.put(deviceAddress, currentBytes.concatBLESegment(byteArray))
        }
    }

    fun isDeviceReceiveComplete(bytes: ByteArray): Boolean {
        // Need to have class byte and 2 length bytes at a minimum to get those values
        if (bytes.size < 3) return false
        val expectedLength = bytes.expectedPacketLengthForBytes() + 3 // add 2 for length bytes themselves and 1 for the class
        val complete = bytes.size == expectedLength
        if (!complete) {
            Timber.e("ERROR: Data length not expected length ${bytes.size} expected: $expectedLength")
        }
        return complete
    }

    fun handlePacketsBytesComplete(deviceAddress: String) {
        val receivedBytes = packetBytesMap.getOrDefault(deviceAddress, byteArrayOf())
        // First reconfirm we got a proper length...
        Timber.i("Completed receive of ${receivedBytes.size} bytes")
        if (isDeviceReceiveComplete(receivedBytes)) {
            reset(deviceAddress)
            listener.onReceivedMessageBytes(deviceAddress, receivedBytes)
        } else {
            Timber.i("Data length not expected length")
        }
    }

    fun reset(deviceAddress: String) {
        packetBytesMap.put(deviceAddress, byteArrayOf())
    }

}

fun ByteArray.isFirstBLESegment(): Boolean {
    // Bit 0 is first segment bit
    return (this.size > 0) and ((this[0].toInt() and 1) == 1)
}

fun ByteArray.isLastBLESegment(): Boolean {
    // Bit 1 is last segment bit
    return (this.size > 0) and ((this[0].toInt() and 2) == 2)
}

fun ByteArray.withoutSegmentHeader(): ByteArray {
    return if (size > 1) copyOfRange(1, this.size) else byteArrayOf()
}

fun ByteArray.concatBLESegment(byteArray: ByteArray): ByteArray {
    return listOf(this, byteArray.withoutSegmentHeader()).merge()
}

fun ByteArray.expectedPacketLengthForBytes(): Int {
    // Adding 2 to include the header bytes that are included in the array
    return ((this[1] and 0xFF).toUInt() + ((this[2] and 0xFF).toUInt() shl 8)).toInt()
}
