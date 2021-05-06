/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.ghs

import com.philips.btclient.acom.Observation

/**
 * Listener interface for Generic Health Sensor devices with the proposed Bluetooth SIG Generic Health Sensor Service
 */
interface GenericHealthSensorHandlerListener {

    /**
     * Called when a blood pressure measurement is received
     *
     * @param deviceAddress Address of the device.
     */
    fun onReceivedObservations(deviceAddress: String, observations: List<Observation>)
}