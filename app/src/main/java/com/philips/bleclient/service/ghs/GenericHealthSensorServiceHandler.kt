/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.acom.AcomObject
import com.philips.bleclient.asHexString
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

class GenericHealthSensorServiceHandler : ServiceHandler(), GenericHealthSensorSegmentListener {

    private val segmentHandler = GenericHealthSensorSegmentHandler(this)

    var listeners: MutableList<GenericHealthSensorHandlerListener> = ArrayList()

    override val name: String
        get() = "GenericHealthSensorServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotificationsAndRead(peripheral, characteristics)
    }

    @ExperimentalStdlibApi
    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        when (characteristic.uuid) {
            OBSERVATION_CHARACTERISTIC_UUID -> handleReceivedObservationBytes(
                peripheral,
                value
            )
            STORED_OBSERVATIONS_CHARACTERISTIC_UUID -> handleStoredObservationBytes(
                peripheral,
                value
            )
            GHS_FEATURES_CHARACTERISTIC_UUID -> handleFeaturesCharacteristics(
                peripheral,
                value
            )
            SIMPLE_TIME_CHARACTERISTIC_UUID -> handleSimpleTime(
                peripheral,
                value
            )
            UNIQUE_DEVICE_ID_CHARACTERISTIC_UUID -> handleUniqueDeviceId(
                peripheral,
                value
            )
        }
    }

    /*
     * GenericHealthSensorHandler Listener methods (add/remove)
     */

    fun addListener(listener: GenericHealthSensorHandlerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: GenericHealthSensorHandlerListener) = listeners.remove(listener)

    /*
     * GenericHealthSensorSegmentListener methods (called when all segments have been received and
     * have a full ACOM object bytes or an error in the received BLE segments
     */

    override fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        val acomObject = AcomObject(byteArray)
        if (acomObject.observations.isNotEmpty()) {
            listeners.forEach { it.onReceivedObservations(deviceAddress, acomObject.observations) }
        }
    }

    override fun onReceivedOutOfSequenceMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        segmentHandler.reset(deviceAddress)
    }

    override fun onReceivedInvalidSegment(
        deviceAddress: String,
        byteArray: ByteArray,
        error: GenericHealthSensorSegmentListener.InvalidSegmentError
    ) {
        segmentHandler.reset(deviceAddress)
    }

    @ExperimentalStdlibApi
    private fun handleReceivedObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(
            name,
            "Received Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral"
        )
        segmentHandler.receiveBytes(peripheral.address, value)
    }

    private fun handleStoredObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "Stored Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleFeaturesCharacteristics(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "Features characteristic update <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleSimpleTime(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "Simple time bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleUniqueDeviceId(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "Unique device ID bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.add(OBSERVATION_CHARACTERISTIC_UUID)
    }

    companion object {
        // Temp assigned GATT Service UUID Allocated for GHS
        val SERVICE_UUID = UUID.fromString("00007f44-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val OBSERVATION_CHARACTERISTIC_UUID =
            UUID.fromString("00007f43-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val STORED_OBSERVATIONS_CHARACTERISTIC_UUID =
            UUID.fromString("00007f42-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val GHS_FEATURES_CHARACTERISTIC_UUID =
            UUID.fromString("00007f41-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val SIMPLE_TIME_CHARACTERISTIC_UUID =
            UUID.fromString("00007f3d-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val UNIQUE_DEVICE_ID_CHARACTERISTIC_UUID =
            UUID.fromString("00007f3a-0000-1000-8000-00805f9b34fb")

    }
}