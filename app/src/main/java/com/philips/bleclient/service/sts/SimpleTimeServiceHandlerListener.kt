package com.philips.bleclient.service.sts

import java.util.*

interface SimpleTimeServiceHandlerListener {


    /**
     * Called when a time is received from the SimpleTimeService
     *
     * @param deviceAddress Address of the device.
     * @param date The date received
     *
     * TODO Need to support tick counters
     */
    fun onReceivedTime(deviceAddress: String, time: Date)

}