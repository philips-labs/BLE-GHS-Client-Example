package com.philips.bleclient.service.ghs

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.merge
import timber.log.Timber
import java.lang.RuntimeException

class GenericHealthSensorPacketHandler(val listener: GenericHealthSensorPacketListener) {

    // TODO Refactor to DRY this code out once we're set with 0.5 spec

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

    fun expectedLengthForBytes(bytes: ByteArray): Int {
        // Adding 2 to include the header bytes that are included in the array
//        return (bytes[0].toUInt() + (bytes[1].toUInt() shl 8)).toInt() + 2
        return (bytes[0].toUInt() + (bytes[1].toUInt() shl 8)).toInt()
    }

    fun isDevicePacketOverflow(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        val expectedLength = expectedLengthForBytes(bytes)
        return bytes.size > expectedLength
    }

    fun isDeviceReceiveComplete(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        val expectedLength = expectedLengthForBytes(bytes) + 2 // add 2 for length bytes themselves
        return bytes.size == expectedLength
    }

    fun isCRCValid(bytes: ByteArray): Boolean {
        return true;
    }

    fun handlePacketsBytesComplete(deviceAddress: String) {
        val receivedBytes = packetBytesMap.getOrDefault(deviceAddress, byteArrayOf())
        // First reconfirm we got a proper length...
        if (!isDeviceReceiveComplete(receivedBytes)) throw RuntimeException("Data length not expected length")

//        if (!isCRCValid(receivedBytes)) {
//            Timber.e("ERROR: Invalid CRC ${receivedBytes.asFormattedHexString()}")
//            return
//        }

        reset(deviceAddress)
        listener.onReceivedMessageBytes(deviceAddress, receivedBytes)
    }

    // Return the data without the first length and last CRC bytes
    fun dataFromPacket(bytes: ByteArray): ByteArray {
        val bytesWithoutLength = bytes.copyOfRange(2, bytes.size)
        return bytesWithoutLength.copyOfRange(0, bytes.size - 2)
    }

    fun handlePacketOverflow(deviceAddress: String) {
        val receivedBytes = packetBytesMap.getOrDefault(deviceAddress, byteArrayOf())
        listener.onReceiveBytesOverflow(deviceAddress, receivedBytes)
        reset(deviceAddress)
    }

    fun handleReceiveComplete(deviceAddress: String) {
        val receivedBytes = packetBytesMap.getOrDefault(deviceAddress, byteArrayOf())
        reset(deviceAddress)
        listener.onReceivedMessageBytes(deviceAddress, receivedBytes)
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
    return if (size > 2) copyOfRange(1, this.size) else byteArrayOf()
}

fun ByteArray.concatBLESegment(byteArray: ByteArray): ByteArray {
    return listOf(this, byteArray.withoutSegmentHeader()).merge()
}