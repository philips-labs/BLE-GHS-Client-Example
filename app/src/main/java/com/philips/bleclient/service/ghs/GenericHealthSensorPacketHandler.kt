package com.philips.bleclient.service.ghs

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.philips.bleclient.merge
import okhttp3.internal.and
import timber.log.Timber
import java.lang.RuntimeException

class GenericHealthSensorPacketHandler(val listener: GenericHealthSensorPacketListener, val isStored: Boolean) {

    private var packetBytesMap = mutableMapOf<String, ByteArray>()

    fun receiveBytes(deviceAddress: String, byteArray: ByteArray) {
        appendBLESegment(byteArray, deviceAddress)
        if (byteArray.isLastBLESegment()) {
            handlePacketsBytesComplete(deviceAddress)
        } else {
            Timber.i("Received bytes: ${byteArray.asHexString()}")
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
        val expectedLength = bytes.expectedObservationLengthForBytes(isStored)
        val complete : Boolean = bytes.size == expectedLength

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

fun ByteArray.expectedObservationLengthForBytes(isStored: Boolean): Int {
    val lengthFieldOffset = if (isStored) 5 else 1
    val recordNumberLength = if (isStored) 4 else 0
    return ((this[lengthFieldOffset] and 0xFF).toUInt() + ((this[lengthFieldOffset+1] and 0xFF).toUInt() shl 8)).toInt() + recordNumberLength
}
