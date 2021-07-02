/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.btclient.ServiceHandler
import com.philips.btclient.acom.AcomObject
import com.philips.btclient.asHexString
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

class GenericHealthSensorServiceHandler: ServiceHandler(), GenericHealthSensorSegmentListener, GenericHealthSensorAcomBytesListener {

    private val segmentHandler = GenericHealthSensorSegmentHandler(this)
    private val acomBytesHandler = GenericHealthSensorAcomBytesHandler(this)

    var listeners: MutableList<GenericHealthSensorHandlerListener> = ArrayList()

    override val name: String
        get() = "GenericHealthSensorServiceHandler"

    override fun onCharacteristicsDiscovered(peripheral: BluetoothPeripheral, characteristics: List<BluetoothGattCharacteristic>) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotificationsAndRead(peripheral, characteristics)
    }

    @ExperimentalStdlibApi
    override fun onCharacteristicUpdate(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        when (characteristic.uuid) {
            OBSERVATION_CHARACTERISTIC_UUID -> handleReceivedObservationBytes(
                peripheral,
                value
            )
            CONTROL_POINT_CHARACTERISTIC_UUID -> handleControlPoint(
                peripheral,
                value
            )
        }
    }

    // Listener methods

    fun addListener(listener: GenericHealthSensorHandlerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: GenericHealthSensorHandlerListener) = listeners.remove(listener)

    // GenericHealthSensorSegmentListener methods

    override fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        acomBytesHandler.handleReceivedAcomBytes(deviceAddress, byteArray)
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

    // GenericHealthSensorAcomBytesListener

    override fun onReceivedAcomObject(deviceAddress: String, acomObject: AcomObject) {
        if (acomObject.observations.isNotEmpty()) {
            listeners.forEach { it.onReceivedObservations(deviceAddress, acomObject.observations) }
        }
    }

    override fun onAcomError(
        deviceAddress: String,
        byteArray: ByteArray,
        error: GenericHealthSensorAcomBytesListener.ObservationError
    ) {
        Timber.i(name, "onAcomError for device <$deviceAddress> error: $error")
    }

    @ExperimentalStdlibApi
    private fun handleReceivedObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "Received Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        segmentHandler.receiveBytes(peripheral.address, value)
    }

    private fun handleControlPoint(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i(name, "ControlPoint update <${value.asHexString()}> for peripheral: $peripheral")
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.add(OBSERVATION_CHARACTERISTIC_UUID)
    }

    companion object {
        // Using 0x183D as the GATT Service Allocated UUID since it's the next 16-bit available based on the BT SIG doc
        val SERVICE_UUID = UUID.fromString("0000183D-0000-1000-8000-00805f9b34fb")
        // Using 0x2AC4 (object properties) based on the BT SIG doc for GATT Characteristic and Object Type
        // This could also be called ACOM_CHARACTERISTIC_UUID as we're actually receiving ACOM objects
        val OBSERVATION_CHARACTERISTIC_UUID = UUID.fromString("00002AC4-0000-1000-8000-00805f9b34fb")
        // Using 0x2AC6 (object list control point) based on the BT SIG doc for GATT Characteristic and Object Type
        val CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002AC6-0000-1000-8000-00805f9b34fb")
    }
}