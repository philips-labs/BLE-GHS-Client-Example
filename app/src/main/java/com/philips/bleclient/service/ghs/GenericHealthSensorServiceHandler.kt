/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.service.ghs

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.philips.bleclient.*
import com.philips.bleclient.observations.Observation
import com.philips.bleclient.ui.ObservationLog
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.*
import timber.log.Timber
import java.util.*

class GenericHealthSensorServiceHandler : ServiceHandler(), ServiceHandlerManagerListener {

    private val peripherals = mutableListOf<BluetoothPeripheral>()
    private val listeners = mutableListOf<GenericHealthSensorHandlerListener>()
    private val racpListeners = mutableListOf<GenericHealthSensorHandlerRacpListener>()

    var observationHandler = GhsObservationHandler(this)
    var storedObservationHandler = GhsObservationHandler(this, true)
    var controlPointHandler = GhsControlPointHandler(this)
    var racpHandler = GhsRacpHandler(this)
    var featuresHandler = GhsFeaturesHandler(this)
    var securityLevelsHandler = GhsSecurityLevelsHandler(this)

    val observationScheduleDescriptorsInfo = mutableMapOf<String, MutableMap<ObservationType, BluetoothGattDescriptor>>()

    override val name: String
        get() = "GenericHealthSensorServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        readSecurityLevels(peripheral)
        // to do: check if current level is sufficient
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
        enableLiveObservations(peripheral)
        readFeatures(peripheral)
        parseObservationScheduleDescriptors(peripheral)
    }

    private fun parseObservationScheduleDescriptors(peripheral: BluetoothPeripheral) {
        getObservationScheduleDescriptors(peripheral)?.forEach { peripheral.readDescriptor(it) }
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        if (status == GattStatus.SUCCESS) {
            when (characteristic.uuid) {
                OBSERVATION_CHARACTERISTIC_UUID -> observationHandler.handleBytes(peripheral, value)
                STORED_OBSERVATIONS_CHARACTERISTIC_UUID -> storedObservationHandler.handleBytes(peripheral, value)
                GHS_FEATURES_CHARACTERISTIC_UUID -> featuresHandler.handleBytes(peripheral, value)
                UNIQUE_DEVICE_ID_CHARACTERISTIC_UUID -> handleUniqueDeviceId(peripheral, value)
                GHS_CONTROL_POINT_CHARACTERISTIC_UUID -> controlPointHandler.handleBytes(peripheral, value)
                RACP_CHARACTERISTIC_UUID -> racpHandler.handleBytes(peripheral, value)
                OBSERVATION_SCHEDULE_CHANGED_CHARACTERISTIC_UUID -> handleObservationScheduledCharChanged(peripheral, value)
                LE_GATT_SECURITY_LEVELS_UUID -> securityLevelsHandler.handleBytes(peripheral, value)
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

    fun addRacpListener(listener: GenericHealthSensorHandlerRacpListener) {
        if (!racpListeners.contains(listener)) {
            racpListeners.add(listener)
        }
    }

    fun removeRacpListener(listener: GenericHealthSensorHandlerRacpListener) = racpListeners.remove(listener)

    /*
     * GenericHealthSensorHandler RACP Methods
     * (Delegated to the RACP Handler)
     * TODO These methods end up with calls that look for the first connected peripheral..
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
        racpHandler.getAllRecords()
        ObservationLog.log("RACP: Get all records sent")
    }

    fun getFirstRecord() {
        racpHandler.getFirstRecord()
        ObservationLog.log("RACP: Get first record sent")
    }

    fun getLastRecord() {
        racpHandler.getLastRecord()
        ObservationLog.log("RACP: Get last record sent")
    }

    fun getRecordsAbove(recordNumber: Int) {
        racpHandler.getRecordsAbove(recordNumber)
        ObservationLog.log("RACP: Get all records greater than $recordNumber sent")
    }

    fun abortGetRecords() {
        ObservationLog.log("RACP: Aborting get records")
        racpHandler.abortGetRecords()
    }

    fun deleteAllRecords() {
        ObservationLog.log("RACP: Delete all records sent")
        racpHandler.deleteAllRecords()
    }

    fun deleteRecordsAbove(recordNumber: Int) {
        racpHandler.deleteRecordsAbove(recordNumber)
        ObservationLog.log("RACP: Delete all records greater than $recordNumber sent")
    }

    /*
     * RACP callbacks
     */

    fun onNumberOfStoredRecordsResponse(deviceAddress: String, numberOfRecords: Int) {
        Timber.i("RACP Number of stored records: $numberOfRecords for peripheral: $deviceAddress")
        ObservationLog.log("RACP: Number of stored records $numberOfRecords ")
        racpListeners.forEach { it.onNumberOfStoredRecordsResponse(deviceAddress, numberOfRecords) }
    }

    fun onDeleteStoredRecordsResponse(deviceAddress: String, numberOfRecords: Int) {
        Timber.i("RACP Delete of stored records: $numberOfRecords for peripheral: $deviceAddress")
        ObservationLog.log("RACP: Delete of stored records $numberOfRecords ")
        racpListeners.forEach { it.onNumberOfStoredRecordsResponse(deviceAddress, numberOfRecords) }
    }

    fun onNumberOfStoredRecordsRetrieved(deviceAddress: String, numberOfRecords: Int) {
        Timber.i("RACP Number of retrieved records: $numberOfRecords for peripheral: $deviceAddress")
        ObservationLog.log("RACP: Number of retrieved records $numberOfRecords ")
        racpListeners.forEach { it.onNumberOfStoredRecordsRetrieved(deviceAddress, numberOfRecords) }
    }

    fun onRacpAbortCompleted(deviceAddress: String) {
        Timber.i("RACP Abort completed successfully")
        ObservationLog.log("RACP Abort completed successfully")
        racpListeners.forEach { it.onRacpAbortCompleted(deviceAddress) }
    }

    fun onRacpAbortError(deviceAddress: String, code: Byte) {
        Timber.i("RACP Abort failed")
        ObservationLog.log("RACP Abort failed")
        racpListeners.forEach { it.onRacpAbortError(deviceAddress, code) }
    }

    /*
     * Observation support
     */

    fun receivedObservation(deviceAddress: String, observation: Observation) {
        Timber.i("GHS Service Hander rececied Observation: $observation from: $deviceAddress")
        listeners.forEach { it.onReceivedObservations(deviceAddress, listOf(observation)) }
    }

    fun receivedStoredObservation(deviceAddress: String, observation: Observation) {
        racpListeners.forEach { it.onReceivedStoredObservation(deviceAddress, observation) }
    }

    fun receivedSupportedTypes(deviceAddress: String, supportedTypes: List<ObservationType>) {
        listeners.forEach { it.onSupportedObservationTypes(deviceAddress, supportedTypes) }
    }

    fun enableLiveObservations(peripheral: BluetoothPeripheral) {
        write(peripheral, GHS_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(START_SEND_LIVE_OBSERVATIONS))
    }

    fun disableLiveObservations(peripheral: BluetoothPeripheral) {
        write(peripheral, GHS_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(STOP_SEND_LIVE_OBSERVATIONS))
    }

    fun debugObservationScheduleDescriptors(peripheral: BluetoothPeripheral) {
        getObservationScheduleDescriptors(peripheral)?.forEach {
            peripheral.readDescriptor(it)
        }
    }


    fun getObservationScheduleDescriptors(peripheral: BluetoothPeripheral): List<BluetoothGattDescriptor>? {
        val observationScheduleDescriptors = peripheral.getCharacteristic(serviceUUID, GHS_FEATURES_CHARACTERISTIC_UUID)?.descriptors
        return observationScheduleDescriptors
            ?.filter { it.uuid == OBSERVATION_SCHEDULE_DESCRIPTOR_UUID }
    }

    fun writeObservationSchedule(peripheral: BluetoothPeripheral,
                                 measurementPeriod: Float,
                                 updateInterval: Float) {
        Timber.i("observationScheduleDescriptorsInfo for ${peripheral.address}: ${observationScheduleDescriptorsInfo[peripheral.address]?.keys}")
        val info =  observationScheduleDescriptorsInfo.get(peripheral.address)
        if (info == null) {
            getObservationScheduleDescriptors(peripheral)
            Timber.i("refreshed observationScheduleDescriptorsInfo: ${observationScheduleDescriptorsInfo[peripheral.address]?.keys}")
        }
        observationScheduleDescriptorsInfo.get(peripheral.address)?.forEach {
            if (it.key != ObservationType.UNKNOWN) {
                val parser = BluetoothBytesParser()
                parser.setIntValue(it.key.value, BluetoothBytesParser.FORMAT_UINT32)
                parser.setFloatValue(measurementPeriod, 3)
                parser.setFloatValue(updateInterval, 3)
                val result = peripheral.writeDescriptor(it.value, parser.value)
                val debugString =
                    "${if (result) "SUCCESS" else "FAILED"} write Observation Schedule type: ${it.key} period: $measurementPeriod interval: $updateInterval"
                Timber.i(debugString)
                ObservationLog.log(debugString)
            } else {
                Timber.i("No observation schedule to write yet...")
            }
        }
    }


    fun saveObservationScheduleDescriptorInfo(
        peripheral: BluetoothPeripheral,
        descriptor: BluetoothGattDescriptor,
        observationType: ObservationType
    ) {
        val descMap = observationScheduleDescriptorsInfo.getOrPut(peripheral.address) {
            Timber.i("Creating new entry in observationScheduleDescriptorsInfo for peripheral: ${peripheral.address}")
            mutableMapOf()
        }
        Timber.i("Add Observation Scheulde Changed Descriptor for type: $observationType")
        descMap.put(observationType, descriptor)
        observationScheduleDescriptorsInfo.put(peripheral.address, descMap)
    }

    override fun onDescriptorRead(
        peripheral: BluetoothPeripheral,
        value: ByteArray?,
        descriptor: BluetoothGattDescriptor,
        status: GattStatus
    ) {
        Timber.i("onDescriptorRead OBSERVATION_SCHEDULE_DESCRIPTOR_UUID value: ${value?.asHexString() ?: null.toString()}")
        value?.let {
            val obsType = readObservationScheduleChangedBytes(value, "onDescriptorRead")
            saveObservationScheduleDescriptorInfo(peripheral, descriptor, obsType)
        }
        super.onDescriptorRead(peripheral, value, descriptor, status)
    }

    private fun readObservationScheduleChangedBytes(value: ByteArray, debugPrefix: String = ""): ObservationType {
        if (value.size == 12) {
            val parser = BluetoothBytesParser(value)
            val obsType = ObservationType.fromValue(parser.getUInt32())
            val measurementPeriod = parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val updateInterval = parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val debugString =
                "$debugPrefix type: $obsType measurementPeriod: $measurementPeriod updateInterval: $updateInterval"
            Timber.i(debugString)
            ObservationLog.log(debugString)
            return obsType
        } else {
            Timber.i("Invalid schedule descriptor")
            return ObservationType.UNKNOWN
        }
    }

    override fun onDescriptorWrite(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: GattStatus
    ) {
        if (descriptor.uuid == OBSERVATION_SCHEDULE_DESCRIPTOR_UUID) {
            Timber.i("onDescriptorWrite OBSERVATION_SCHEDULE_DESCRIPTOR_UUID value: ${value.asHexString()}")
//            peripheral.readDescriptor(descriptor)
        } else {
            Timber.i("onDescriptorWrite uuid: ${descriptor.uuid} value: ${value.asHexString()}")
        }
    }

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("GHS Service Handler: Connected Peripheral ${peripheral.address}")
        peripherals.add(peripheral)
        listeners.forEach { it.onConnected(peripheral.address) }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("GHS Service Handler: Disconnected Peripheral ${peripheral.address}")
        peripherals.remove(peripheral)
        listeners.forEach { it.onDisconnected(peripheral.address) }
    }

    private fun handleObservationScheduledCharChanged(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Observation Schedule Char Update bytes: <${value.asHexString()}> for peripheral: $peripheral")
        readObservationScheduleChangedBytes(value, "Observation schedule Char changed")
    }

    private fun handleUniqueDeviceId(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i( "Unique device ID bytes: <${value.asHexString()}> for peripheral: $peripheral")
    }

    private fun readFeatures(peripheral: BluetoothPeripheral) {
        read(peripheral, GHS_FEATURES_CHARACTERISTIC_UUID)
    }

    private fun readSecurityLevels(peripheral: BluetoothPeripheral) {
        read(peripheral, LE_GATT_SECURITY_LEVELS_UUID)
    }

    private fun read(peripheral: BluetoothPeripheral, characteristicUUID: UUID) {
        peripheral.getCharacteristic(SERVICE_UUID, characteristicUUID)?.let {
            val result = peripheral.readCharacteristic(it)
        }
    }

    fun write(characteristicUUID: UUID, value: ByteArray) {
//        val connectedPeripherals = peripherals
        val connectedPeripherals = getCurrentCentrals()
        if (connectedPeripherals.isEmpty()) {
            Timber.i("GHS Service Handler: No connected periperals connected to write characteristic $characteristicUUID")
        } else {
            write(connectedPeripherals.first(), characteristicUUID, value)
        }
    }

    fun sendInvalidCommand(p: BluetoothPeripheral) {
        write(p, GHS_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(INVALID_GHSCP_COMMAND))

    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(arrayOf(
            OBSERVATION_CHARACTERISTIC_UUID,
            STORED_OBSERVATIONS_CHARACTERISTIC_UUID,
            GHS_FEATURES_CHARACTERISTIC_UUID,
            GHS_CONTROL_POINT_CHARACTERISTIC_UUID,
            OBSERVATION_SCHEDULE_CHANGED_CHARACTERISTIC_UUID,
            RACP_CHARACTERISTIC_UUID))
        ServiceHandlerManager.instance?.addListener(this)
    }

    companion object {

        // Temp assigned GATT Service UUID Allocated for GHS
        val SERVICE_UUID: UUID = UUID.fromString("00007f44-0000-1000-8000-00805f9b34fb")

        // Temp assigned GATT Characteristic UUID Allocated for GHSm
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

        val OBSERVATION_SCHEDULE_CHANGED_CHARACTERISTIC_UUID =
            UUID.fromString("00007f3f-0000-1000-8000-00805f9b34fb")

        val OBSERVATION_SCHEDULE_DESCRIPTOR_UUID =
            UUID.fromString("00007f35-0000-1000-8000-00805f9b34fb")

        val VALID_RANGE_AND_ACCURACY_DESCRIPTOR_UUID =
            UUID.fromString("00007f34-0000-1000-8000-00805f9b34fb")

        val LE_GATT_SECURITY_LEVELS_UUID =
            UUID.fromString("00002BF5-0000-1000-8000-00805f9b34fb")

        val instance: GenericHealthSensorServiceHandler? get() {
            return ServiceHandlerManager.instance?.serviceHandlerForUUID(SERVICE_UUID)?.let { it as GenericHealthSensorServiceHandler }
        }

        /*
         * GHS Control Point commands and status values
         */
        private const val START_SEND_LIVE_OBSERVATIONS = 0x01.toByte()
        private const val STOP_SEND_LIVE_OBSERVATIONS = 0x02.toByte()
        private const val INVALID_GHSCP_COMMAND = 0x03.toByte()
        private const val CONTROL_POINT_SUCCESS = 0x80.toByte()
        private const val CONTROL_POINT_SERVER_BUSY = 0x81.toByte()
        private const val CONTROL_POINT_ERROR_LIVE_OBSERVATIONS = 0x82.toByte()
    }
}