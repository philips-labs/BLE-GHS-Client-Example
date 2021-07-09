/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

interface GenericHealthSensorSegmentListener {

    enum class InvalidSegmentError {
        Length,
        Header,
        OutOfSequence
    }

    /**
     * Called when a full message payload (of one or more segments) has been received
     *
     * @param byteArray message payload.
     */
    fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray)

    /**
     * Called when a out of sequence segment has been received
     *
     * @param byteArray message payload.
     */
    fun onReceivedOutOfSequenceMessageBytes(deviceAddress: String, byteArray: ByteArray)

    /**
     * Called when a segment with an invalid header has been received
     *
     * @param byteArray message payload.
     */
    fun onReceivedInvalidSegment(deviceAddress: String, byteArray: ByteArray, error: InvalidSegmentError)

}