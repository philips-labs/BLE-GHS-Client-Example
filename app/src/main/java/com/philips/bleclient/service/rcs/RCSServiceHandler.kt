package com.philips.bleclient.service.rcs

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import com.philips.bleclient.*
import com.philips.bleclient.extensions.*
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import com.welie.blessed.WriteType
import timber.log.Timber
import java.util.*

class RCSServiceHandler : ServiceHandler(),
    ServiceHandlerManagerListener {

    var listeners: MutableSet<RCSServiceHandlerListener> = mutableSetOf()
    private val peripherals = mutableSetOf<BluetoothPeripheral>()

    override val name: String
        get() = "RCSServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
        readRCSFeatures(peripheral)
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        //super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        if (status == GattStatus.SUCCESS) {
            when (characteristic.uuid) {
                RC_CONTROL_POINT_CHARACTERISTIC_UUID -> {
                    // parse the response and check for success or failure
                    Timber.i("RC_CONTROL_POINT_CHARACTERISTIC_UUID update received: ${value.asHexString()}")
                }
                RC_SETTINGS_CHARACTERISTIC_UUID -> {
                    // parse and check the ready for disconnect bit
                    //Timber.i("RC_SETTINGS_CHARACTERISTIC_UUID update received: ${value.asHexString()}")
                    val parser = BluetoothBytesParser(value)
                    val length = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                    val settings1 = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                    val readyForDisconnect = (settings1 and READY_FOR_DISCONNECT_FLAG != 0)
                    Timber.i("RC_SETTINGS_CHARACTERISTIC_UUID update received: ${value.asHexString()} Length: $length, ready for disconnect: $readyForDisconnect")

                }
                RC_FEATURE_CHARACTERISTIC_UUID -> {
                    Timber.i("RC_SETTINGS_CHARACTERISTIC_UUID value received: ${value.asHexString()}")
                    val parser = BluetoothBytesParser(value)
                    val crc = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
                    val features1 = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                    if (features1 and CRC_SUPPORTED != 0) {
                        Timber.i("RCS CRC Supported.")
                    }
                    if (features1 and ENABLE_DISCONNECT_SUPPORTED != 0){
                        Timber.i("RCS Enable Disconnect Supported.")
                    }
                    if (features1 and READY_FOR_DISCONNECT_SUPPORTED != 0) {
                        Timber.i( "RCS Ready For Disonnect Supported.")
                    }

                }
                else -> {
                    Timber.i("RCS Unexpected characteristic update received UUID:${characteristic.uuid}, value: ${value.asHexString()}")
                }

            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    fun enableDisconnect(peripheral: BluetoothPeripheral){
        // To do: send enable disconnect command using RCS control point
        if (peripheral.writeCharacteristic(serviceUUID, RC_CONTROL_POINT_CHARACTERISTIC_UUID, byteArrayOf(0x00), WriteType.WITH_RESPONSE) ) {
            Timber.i("Writing to RCS CP: enable disconnect.")
        } else {
            Timber.i("Failed to write to RCS CP.")
        }
    }

    fun readRCSFeatures(peripheral: BluetoothPeripheral){
        if (peripheral.readCharacteristic(serviceUUID, RC_FEATURE_CHARACTERISTIC_UUID) ) {
            Timber.i("Reading RC_FEATURE_CHARACTERISTIC_UUID")
        } else {
            Timber.i("Failed to read RC_FEATURE_CHARACTERISTIC_UUID")
        }
    }


    /*
     * RCSServiceHandler Listener methods (add/remove)
     */

    fun addListener(listener: RCSServiceHandlerListener) = listeners.add(listener)

    fun removeListener(listener: RCSServiceHandlerListener) = listeners.remove(listener)

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("RCS Service Handler: Connected Peripheral ${peripheral.address}")
        peripherals.add(peripheral)
        listeners.forEach { it.onConnected(peripheral.address) }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("RCS Service Handler: Disconnected Peripheral ${peripheral.address}")
        peripherals.remove(peripheral)
        listeners.forEach { it.onDisconnected(peripheral.address) }
    }


    init {
        serviceUUID = RCS_SERVICE_UUID
        supportedCharacteristics.add(RC_FEATURE_CHARACTERISTIC_UUID)
        supportedCharacteristics.add(RC_SETTINGS_CHARACTERISTIC_UUID)
        supportedCharacteristics.add(RC_CONTROL_POINT_CHARACTERISTIC_UUID)
        ServiceHandlerManager.instance?.addListener(this)
    }

    companion object {
        val RCS_SERVICE_UUID = UUID.fromString("00001829-0000-1000-8000-00805f9b34fb")
        val RC_FEATURE_CHARACTERISTIC_UUID =
            UUID.fromString("00002B1D-0000-1000-8000-00805f9b34fb")
        val RC_SETTINGS_CHARACTERISTIC_UUID =
            UUID.fromString("00002B1E-0000-1000-8000-00805f9b34fb")
        val RC_CONTROL_POINT_CHARACTERISTIC_UUID =
            UUID.fromString("00002B1F-0000-1000-8000-00805f9b34fb")

        val READY_FOR_DISCONNECT_FLAG = 16
        val CRC_SUPPORTED = 1
        val ENABLE_DISCONNECT_SUPPORTED = 2
        val READY_FOR_DISCONNECT_SUPPORTED = 4
    }
}