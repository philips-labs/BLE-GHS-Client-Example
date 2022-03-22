/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import com.philips.bleclient.acom.Observation
import com.philips.btserver.generichealthservice.ObservationType

/**
 * Listener interface for Generic Health Sensor devices with the proposed Bluetooth SIG Generic Health Sensor Service
 */
interface GenericHealthSensorHandlerListener {

    /**
     * Called when a collection of observations are received
     *
     * @param deviceAddress Address of the device.
     * @param observations List of new recevied observations
     */
    fun onReceivedObservations(deviceAddress: String, observations: List<Observation>)


    /**
     * Called when the list of supported device specializations (supported types) have been received
     * See the GHS specfication section 3.1.1.3
     *
     * @param deviceAddress Address of the device.
     * @param deviceAddress Address of the device.
     */
    fun onSupportedObservationTypes(deviceAddress: String, observationTypes: List<ObservationType>)

}