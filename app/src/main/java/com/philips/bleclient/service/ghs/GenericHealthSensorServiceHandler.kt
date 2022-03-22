/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.acom.AcomObject
import com.philips.bleclient.acom.Observation
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.nio.ByteOrder
import java.util.*

class GenericHealthSensorServiceHandler : ServiceHandler(), GenericHealthSensorSegmentListener, GenericHealthSensorPacketListener {

    private val segmentHandler = GenericHealthSensorSegmentHandler(this)
    private val packetHandler = GenericHealthSensorPacketHandler(this)

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
        if (status == GattStatus.SUCCESS) {
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
                UNIQUE_DEVICE_ID_CHARACTERISTIC_UUID -> handleUniqueDeviceId(
                    peripheral,
                    value
                )
            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
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
     * GenericHealthSensorSegmentListener/GenericHealthSensorPacketListener methods (called when all segments have been received and
     * have a full ACOM object bytes or an error in the received BLE segments
     */

    override fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        Timber.i("Received Message of ${byteArray.size} bytes")

        Observation.fromBytes(byteArray)?.let { obs ->
            listeners.forEach { it.onReceivedObservations(deviceAddress, listOf(obs)) }
        }

//        val acomObject = AcomObject(byteArray)
//        if (acomObject.observations.isNotEmpty()) {
//            listeners.forEach { it.onReceivedObservations(deviceAddress, acomObject.observations) }
//        }
    }

    override fun onReceiveBytesOverflow(deviceAddress: String, byteArray: ByteArray) {
        Timber.e("Error BYTES OVERFLOW: $deviceAddress bytes: <${byteArray.asFormattedHexString()}>")
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
        Timber.i("Received Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
//        segmentHandler.receiveBytes(peripheral.address, value)
        packetHandler.receiveBytes(peripheral.address, value)
    }

    private fun handleStoredObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Stored Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleFeaturesCharacteristics(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Features characteristic update bytes: <${value.asFormattedHexString()}> for peripheral: ${peripheral.address}")
        val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
        val numberOfObservations = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        // Ensure the number of bytes matches what we expect
        val supportedObs = mutableListOf<ObservationType>()
        if (value.size == (numberOfObservations * 4) + 1) {
            repeat (numberOfObservations) {
                supportedObs.add(ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)))
            }
            Timber.i( "Features characteristic update received obs: <$supportedObs>")
            listeners.forEach { it.onSupportedObservationTypes(peripheral.address, supportedObs) }
        } else {
            Timber.i( "Error in features characteristic bytes size: ${value.size} expected: ${(numberOfObservations * 4) + 1}")
        }
    }

    private fun handleSimpleTime(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Simple time bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleUniqueDeviceId(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Unique device ID bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(arrayOf(OBSERVATION_CHARACTERISTIC_UUID, GHS_FEATURES_CHARACTERISTIC_UUID))
    }

    companion object {
        // Temp assigned GATT Service UUID Allocated for GHS
        val SERVICE_UUID: UUID = UUID.fromString("00007f44-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val OBSERVATION_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00007f43-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val STORED_OBSERVATIONS_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00007f42-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val GHS_FEATURES_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00007f41-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val UNIQUE_DEVICE_ID_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00007f3a-0000-1000-8000-00805f9b34fb")

    }
}