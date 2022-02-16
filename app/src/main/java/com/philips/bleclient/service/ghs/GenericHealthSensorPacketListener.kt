package com.philips.bleclient.service.ghs

interface GenericHealthSensorPacketListener {

    /**
     * Called when a full message payload (of one or more packets) has been received
     *
     * @param byteArray message payload.
     */
    fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray)

    /**
     * Called when a packet overflows the expected number of bytes
     *
     * @param byteArray message payload.
     */
    fun onReceiveBytesOverflow(deviceAddress: String, byteArray: ByteArray)

}