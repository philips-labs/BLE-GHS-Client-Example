/*
 * Copyright (c) Koninklijke Philips N.V., 2020.
 * All rights reserved.
 */

package com.philips.bleclient.ghs

import kotlin.experimental.and

class GenericHealthSensorSegmentHandler(val listener: GenericHealthSensorSegmentListener) {

    private var messageBytesMap = mutableMapOf<String, GenericHealthSensorMessageBytes>()

    @ExperimentalStdlibApi
    fun receiveBytes(deviceAddress: String, byteArray: ByteArray) {
        require(byteArray.size > 1) {
            reset(deviceAddress)
            listener.onReceivedInvalidSegment(deviceAddress, byteArray, GenericHealthSensorSegmentListener.InvalidSegmentError.Length)
        }

        when(byteArray[0].and(0x03)) {
            HEADER_SINGLE_SEGMENT -> handleSingleSegment(deviceAddress, byteArray)
            HEADER_FIRST_SEGMENT -> handleSegment(deviceAddress, byteArray)
            HEADER_NEXT_SEGMENT -> handleNextSegment(deviceAddress, byteArray)
            HEADER_LAST_SEGMENT -> handleLastSegment(deviceAddress, byteArray)
            else -> handleInvalidSegment(deviceAddress, byteArray, GenericHealthSensorSegmentListener.InvalidSegmentError.Header)
        }
    }

    fun reset(deviceAddress: String) {
        messageBytesMap.put(deviceAddress, GenericHealthSensorMessageBytes())
    }

    @ExperimentalStdlibApi
    private fun handleSingleSegment(deviceAddress: String, byteArray: ByteArray) {
        handleSegment(deviceAddress, byteArray)
        receiveMessageComplete(deviceAddress)
    }

    @ExperimentalStdlibApi
    private fun handleSegment(deviceAddress: String, byteArray: ByteArray) {
        setLastSequenceNumberFor(deviceAddress, headerSequenceNumber(byteArray))
        appendSegment(deviceAddress, byteArray)
    }

    @ExperimentalStdlibApi
    private fun handleNextSegment(deviceAddress: String, byteArray: ByteArray) {
        if (isInSequence(deviceAddress, byteArray)) {
            handleSegment(deviceAddress, byteArray)
        } else {
            handleInvalidSegment(deviceAddress, byteArray, GenericHealthSensorSegmentListener.InvalidSegmentError.OutOfSequence)
        }
    }

    private fun handleInvalidSegment(deviceAddress: String, byteArray: ByteArray, error: GenericHealthSensorSegmentListener.InvalidSegmentError) {
        listener.onReceivedInvalidSegment(deviceAddress, byteArray, error)
        reset(deviceAddress)
    }

    @ExperimentalStdlibApi
    private fun handleLastSegment(deviceAddress: String, byteArray: ByteArray) {
        if (isInSequence(deviceAddress, byteArray)) {
            appendSegment(deviceAddress, byteArray)
            receiveMessageComplete(deviceAddress)
        } else {
            handleInvalidSegment(deviceAddress, byteArray, GenericHealthSensorSegmentListener.InvalidSegmentError.OutOfSequence)
        }
    }

    private fun appendSegment(deviceAddress: String, byteArray: ByteArray) {
        messageBytesFor(deviceAddress).receivedMessageBytes += segmentPayloadBytes(byteArray)
    }

    @ExperimentalStdlibApi
    private fun headerSequenceNumber(byteArray: ByteArray) : Byte {
        val seqNumber = byteArray[0] and HEADER_SEGMENT_NUMBER_BIT_MASK
        return seqNumber.rotateRight(2)
    }

    private fun receiveMessageComplete(deviceAddress: String) {
        val receivedBytes = messageBytesFor(deviceAddress).receivedMessageBytes
        reset(deviceAddress)
        listener.onReceivedMessageBytes(deviceAddress, receivedBytes)
    }

    @ExperimentalStdlibApi
    private fun isInSequence(deviceAddress: String, byteArray: ByteArray) : Boolean {
        return headerSequenceNumber(byteArray) == nextSequenceNumber(deviceAddress)
    }

    private fun nextSequenceNumber(deviceAddress: String) : Byte {
        return if (lastSequenceNumberFor(deviceAddress) == HEADER_MAX_SEQUENCE_NUMBER) 0x0.toByte() else (lastSequenceNumberFor(deviceAddress) + 1).toByte()
    }

    private fun messageBytesFor(deviceAddress: String): GenericHealthSensorMessageBytes {
        return messageBytesMap.getOrPut(deviceAddress) { GenericHealthSensorMessageBytes() }
    }

    private fun lastSequenceNumberFor(deviceAddress: String): Byte {
        return messageBytesFor(deviceAddress).lastSequenceNumber
    }

    private fun setLastSequenceNumberFor(deviceAddress: String, sequenceNumber: Byte) {
        messageBytesFor(deviceAddress).lastSequenceNumber = sequenceNumber
    }

    private fun segmentPayloadBytes(byteArray: ByteArray) : ByteArray {
        return byteArray.copyOfRange(1, byteArray.size)
    }

    companion object {
        private const val HEADER_SEGMENT_NUMBER_BIT_MASK: Byte = 0xFC.toByte()
        private const val HEADER_MAX_SEQUENCE_NUMBER: Byte = 0x3F.toByte()

        private const val HEADER_NEXT_SEGMENT : Byte = 0x00.toByte()
        private const val HEADER_FIRST_SEGMENT : Byte = 0x01.toByte()
        private const val HEADER_LAST_SEGMENT : Byte = 0x02.toByte()
        private const val HEADER_SINGLE_SEGMENT : Byte = 0x03.toByte()

    }

}