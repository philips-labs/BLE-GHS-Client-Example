package com.philips.bleclient.service.ets

import java.util.*

interface ElapsedTimeServiceHandlerListener {

    /**
     * Called when a time is received from the ElapsedTimeService
     *
     * @param deviceAddress Address of the device.
     * @param date The date received
     *
     * TODO Need to support tick counters
     */
    fun onReceivedTime(deviceAddress: String, time: Date) {}

    fun onReceivedEtsBytes(deviceAddress: String, bytes: ByteArray) {}


}