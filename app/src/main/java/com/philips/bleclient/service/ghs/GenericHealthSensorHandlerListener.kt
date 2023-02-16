/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import com.philips.bleclient.observations.Observation
import com.philips.bleclient.ui.GHSDeviceInfoMap
import com.philips.bleclient.ui.ObservationLog
import com.philips.bleclient.ui.PeripheralInfoActivity
import com.philips.btserver.generichealthservice.ObservationType
import timber.log.Timber

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
    fun onReceivedObservations(deviceAddress: String, observations: List<Observation>) {}

    /**
     * Called when the list of supported device specializations (supported types) have been received
     * See the GHS specfication section 3.1.1.3
     *
     * @param deviceAddress Address of the device.
     * @param observationTypes List of observations types the device supports
     */
    fun onSupportedObservationTypes(deviceAddress: String, observationTypes: List<ObservationType>) {}

    /**
     * Called when a GHS device connects
     *
     * @param deviceAddress Address of the device.
     * @param observations List of new recevied observations
     */
    fun onConnected(deviceAddress: String) {}

    /**
     * Called when a GHS device disconnects
     *
     * @param deviceAddress Address of the device.
     * @param observations List of new recevied observations
     */
    fun onDisconnected(deviceAddress: String) {}

    fun onsupportedDeviceSpecializations(deviceAddress: String, supportedDevSpecs: List<DeviceSpecialization>) {
        GHSDeviceInfoMap.onSupportedDeviceSpecializations(deviceAddress, supportedDevSpecs)
    }

    fun onSupportedDeviceSpecializations(
        deviceAddress: String,
        deviceSpecializations: List<DeviceSpecialization>
    )
}

/**
 * Listener interface for Generic Health Sensor devices with RACP/Stored observations
 */
interface GenericHealthSensorHandlerRacpListener {

    /**
     * Called when a stored observation is received
     *
     * @param deviceAddress Address of the device.
     * @param observation stored recevied observation
     */
    fun onReceivedStoredObservation(deviceAddress: String, observation: Observation)

    /**
     * Called when a response to a number of stored records query is received
     *
     * @param deviceAddress Address of the device.
     * @param numberOfRecords result of query with number of records
     */
    fun onNumberOfStoredRecordsResponse(deviceAddress: String, numberOfRecords: Int)

    /**
     * Called when a combined report with observations is complete (over the stored obs characteritic).
     * Number of records transmitted is param
     *
     * @param deviceAddress Address of the device.
     * @param numberOfRecords number of records transmitted
     */
    fun onNumberOfStoredRecordsRetrieved(deviceAddress: String, numberOfRecords: Int)

    /**
     * Called when a RACP Abort command is completed successfully
     *
     * @param deviceAddress Address of the device.
     */
    fun onRacpAbortCompleted(deviceAddress: String)

    /**
     * Called when a RACP Abort command failed
     *
     * @param deviceAddress Address of the device.
     */
    fun onRacpAbortError(deviceAddress: String, code: Byte)
}