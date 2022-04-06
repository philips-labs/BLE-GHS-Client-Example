/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.*
import com.philips.bleclient.acom.Observation
import com.philips.bleclient.ui.ObservationLog
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import com.welie.blessed.WriteType
import timber.log.Timber
import java.nio.ByteOrder
import java.util.*

class GenericHealthSensorServiceHandler : ServiceHandler(),
    GenericHealthSensorSegmentListener,
    GenericHealthSensorPacketListener,
    ServiceHandlerManagerListener {

    private val segmentHandler = GenericHealthSensorSegmentHandler(this)
    private val packetHandler = GenericHealthSensorPacketHandler(this)
    private val storedObservationPacketHandler = GenericHealthSensorPacketHandler(this)
    private val peripherals = mutableSetOf<BluetoothPeripheral>()

    var listeners: MutableList<GenericHealthSensorHandlerListener> = ArrayList()

    internal val ghsControlPointCharacteristic = BluetoothGattCharacteristic(
        GHS_CONTROL_POINT_CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    var controlPointHandler = GhsControlPointHandler(this)
    var racpHandler = GhsRacpHandler(this)


    internal val racpCharacteristic = BluetoothGattCharacteristic(
        RACP_CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_INDICATE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    override val name: String
        get() = "GenericHealthSensorServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
        enableLiveObservations(peripheral)
        readFeatures(peripheral)
    }

//    @ExperimentalStdlibApi
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
                GHS_CONTROL_POINT_CHARACTERISTIC_UUID -> controlPointHandler.handleResponse(peripheral, value)
                RACP_CHARACTERISTIC_UUID -> racpHandler.handleResponse(peripheral, value)
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
     * GenericHealthSensorHandler RACP Methods
     * (Delegated to the RACP Handler)
     */

    fun getNumberOfRecords() {
        ObservationLog.log("RACP: Get number of records sent")
        racpHandler.getNumberOfRecords()
    }

    fun getNumberOfRecordsGreaterThan(recordNumber: Int) {
        ObservationLog.log("RACP: Get number of records greater than $recordNumber sent")
        racpHandler.getNumberOfRecordsGreaterThan(recordNumber)
    }

    fun getAllRecords() {
        ObservationLog.log("RACP: Get all records sent")
        racpHandler.getAllRecords()
    }

    fun getRecordsAbove(recordNumber: Int) {
        ObservationLog.log("RACP: Get all records greater than $recordNumber sent")
        racpHandler.getRecordsAbove(recordNumber)
    }

    /*
     * GenericHealthSensorSegmentListener/GenericHealthSensorPacketListener methods (called when all segments have been received and
     * have a full ACOM object bytes or an error in the received BLE segments
     */

    override fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        Timber.i("Received Message of ${byteArray.size} bytes")

        Observation.fromBytes(byteArray)?.let { obs ->
            listeners.forEach { it.onReceivedObservations(deviceAddress, listOf(obs)) }
        }
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

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.add(peripheral)
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.remove(peripheral)
    }

    //    @ExperimentalStdlibApi
    private fun handleReceivedObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Received Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
//        segmentHandler.receiveBytes(peripheral.address, value)
        packetHandler.receiveBytes(peripheral.address, value)
    }

    private fun handleStoredObservationBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Stored Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        storedObservationPacketHandler.receiveBytes(peripheral.address, value)
    }

    private fun handleFeaturesCharacteristics(peripheral: BluetoothPeripheral, value: ByteArray) {
        // Handle case where features hasn't been set up on the server (shouldn't happen, but safety)
        if (value.size < 2) return
        Timber.i( "Features characteristic update bytes: <${value.asFormattedHexString()}> for peripheral: ${peripheral.address}")
        val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
        val featuresFlags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        val numberOfObservations = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        // Ensure the number of bytes matches what we expect (Flags, number of types and 4 bytes per type
        if (value.size >= (numberOfObservations * 4) + 2) {
            val supportedObs = mutableListOf<ObservationType>()
            repeat (numberOfObservations) {
                supportedObs.add(ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)))
            }
            Timber.i( "Features characteristic update received obs: <$supportedObs>")
            listeners.forEach { it.onSupportedObservationTypes(peripheral.address, supportedObs) }
        } else {
            Timber.i( "Error in features characteristic bytes size: ${value.size} expected: ${(numberOfObservations * 4) + 1}")
        }
        // Only flag is for Supported Device Specializations field present
        if (featuresFlags > 0) {
            handleFeaturesDeviceSpecializations(parser)
        }
    }

    private fun handleFeaturesDeviceSpecializations(parser: BluetoothBytesParser) {
        val numberOfDeviceSpecializations = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        Timber.i( "Number of device specializations: $numberOfDeviceSpecializations")
        repeat(numberOfDeviceSpecializations, {
            val deviceSpecBytes = parser.getByteArray(3)
            Timber.i( "Device specialization #${it + 1}: 00 08 ${deviceSpecBytes[1].asHexString()} ${deviceSpecBytes[0].asHexString()} ver: ${deviceSpecBytes[2]}")
        })


    }

    private fun handleSimpleTime(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Simple time bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun handleUniqueDeviceId(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Unique device ID bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    fun enableLiveObservations(peripheral: BluetoothPeripheral) {
        write(peripheral, GHS_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(START_SEND_LIVE_OBSERVATIONS))
    }

    fun disableLiveObservations(peripheral: BluetoothPeripheral) {
        write(peripheral, GHS_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(STOP_SEND_LIVE_OBSERVATIONS))
    }

    fun readFeatures(peripheral: BluetoothPeripheral) {
        read(peripheral, GHS_FEATURES_CHARACTERISTIC_UUID)
    }

    fun read(peripheral: BluetoothPeripheral, characteristicUUID: UUID) {
        peripheral.getCharacteristic(GHS_FEATURES_CHARACTERISTIC_UUID, characteristicUUID)?.let {
            val result = peripheral.readCharacteristic(it)
        }
    }

    fun write(peripheral: BluetoothPeripheral, characteristicUUID: UUID, value: ByteArray) {
        peripheral.getCharacteristic(SERVICE_UUID, characteristicUUID)?.let {
            val result = peripheral.writeCharacteristic(it, value, WriteType.WITH_RESPONSE)
            Timber.i( "Write of bytes: <${value.asHexString()}> for peripheral: $peripheral was $result")
        }
    }

    fun writeWithoutResponse(peripheral: BluetoothPeripheral, characteristicUUID: UUID, value: ByteArray) {
        peripheral.getCharacteristic(SERVICE_UUID, characteristicUUID)?.let {
            val result = peripheral.writeCharacteristic(it, value, WriteType.WITHOUT_RESPONSE)
            Timber.i( "Write of bytes: <${value.asHexString()}> for peripheral: $peripheral was $result")
        }
    }

    fun write(characteristicUUID: UUID, value: ByteArray) {
        if (peripherals.size > 0) write(peripherals.first(), characteristicUUID, value)
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(arrayOf(
            OBSERVATION_CHARACTERISTIC_UUID,
            STORED_OBSERVATIONS_CHARACTERISTIC_UUID,
            GHS_FEATURES_CHARACTERISTIC_UUID,
            GHS_CONTROL_POINT_CHARACTERISTIC_UUID,
            RACP_CHARACTERISTIC_UUID))
        ServiceHandlerManager.instance?.addListener(this)
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

        // Temp assigned GATT Characteristic UUID Allocated for GHS
        val GHS_CONTROL_POINT_CHARACTERISTIC_UUID =
            UUID.fromString("00007f40-0000-1000-8000-00805f9b34fb")

        val RACP_CHARACTERISTIC_UUID =
            UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb")

        /*
         * GHS Control Point commands and status values
         */
        private const val START_SEND_LIVE_OBSERVATIONS = 0x01.toByte()
        private const val STOP_SEND_LIVE_OBSERVATIONS = 0x02.toByte()
        private const val CONTROL_POINT_SUCCESS = 0x80.toByte()
        private const val CONTROL_POINT_SERVER_BUSY = 0x81.toByte()
        private const val CONTROL_POINT_ERROR_LIVE_OBSERVATIONS = 0x82.toByte()


    }
}