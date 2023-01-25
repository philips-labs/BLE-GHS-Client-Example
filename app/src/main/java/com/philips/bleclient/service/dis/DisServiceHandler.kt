package com.philips.bleclient.service.dis

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.ServiceHandlerManagerListener
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothBytesParser.FORMAT_UINT64
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.nio.ByteOrder
import java.util.*
import java.util.UUID.*

class DisServiceHandler : ServiceHandler(), ServiceHandlerManagerListener {

    private val listeners = mutableListOf<DisServiceListener>()
    private val peripherals = mutableListOf<BluetoothPeripheral>()

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        readDeviceInformation(peripheral)
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
        if (status == GattStatus.SUCCESS) {
            when (characteristic.uuid) {
                MANUFACTURER_NAME_STRING_CHARACTERISTIC_UUID -> {
                    val name = parser.getStringValue();
                    Timber.i("Manufacturer name read:" + name)
                    DisInfoMap.setManufacturerName(peripheral, name)
                }
                MODEL_NUMBER_STRING_CHARACTERISTIC_UUID -> {
                    val name = parser.getStringValue();
                    Timber.i("Model number read:" + name)
                    DisInfoMap.setModelNumber(peripheral, name)
                }
                SERIAL_NUMBER_STRING_CHARACTERISTIC_UUID -> {
                    val name = parser.getStringValue();
                    Timber.i("Serial number read:" + name)
                    DisInfoMap.setSerialNumber(peripheral, name)
                }
                SYSTEM_ID_STRING_CHARACTERISTIC_UUID -> {
                    val sysId = parser.getLongValue(FORMAT_UINT64);
                    Timber.i("System ID read:" + sysId)
                    DisInfoMap.setSystemId(peripheral, sysId.toString())
                }
                UDI_STRING_CHARACTERISTIC_UUID -> {
                    val name = parser.getStringValue();
                    Timber.i("UDI read:" + name)
                    DisInfoMap.setUDI(peripheral, name)
                }
                else -> Timber.i("DIS characteristic read - UUID:" + characteristic.uuid + " value:" + value.toString())
            }
        } else {
            Timber.i("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    fun readDeviceInformation(peripheral: BluetoothPeripheral){
        supportedCharacteristics.forEach { uuid ->
            val characteristic = peripheral.getCharacteristic(SERVICE_UUID, uuid)
            if (characteristic != null) {
                when (uuid) {
                    MANUFACTURER_NAME_STRING_CHARACTERISTIC_UUID -> {
                        timber.log.Timber.i("Reading Manufacturer name")
                    }
                    MODEL_NUMBER_STRING_CHARACTERISTIC_UUID -> {
                        timber.log.Timber.i("Reading Model number")
                    }
                    SERIAL_NUMBER_STRING_CHARACTERISTIC_UUID -> {
                        timber.log.Timber.i("Reading Serial number")
                    }
                    SYSTEM_ID_STRING_CHARACTERISTIC_UUID -> {
                        timber.log.Timber.i("Reading System ID")
                    }
                    UDI_STRING_CHARACTERISTIC_UUID -> {
                        timber.log.Timber.i("Reading UDI")
                    }
                    else -> {
                        timber.log.Timber.i("Reading characteristic with UUID:$uuid")
                    }
                }
                if (!peripheral.readCharacteristic(characteristic)){
                    timber.log.Timber.i("Read failed!")
                }
            }
        }
    }

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral) {
        // nothing to do here
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("DIS Service Handler: Connected Peripheral ${peripheral.address}")
        peripherals.add(peripheral)
        listeners.forEach { it.onConnected(peripheral.address) }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        super.onDisconnectedPeripheral(peripheral)
        Timber.i("DIS Service Handler: Disconnected Peripheral ${peripheral.address}")
        peripherals.remove(peripheral)
        listeners.forEach { it.onDisconnected(peripheral.address) }
    }

    fun addListener(listener: DisServiceListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(arrayOf(
            MANUFACTURER_NAME_STRING_CHARACTERISTIC_UUID,
            MODEL_NUMBER_STRING_CHARACTERISTIC_UUID,
            SERIAL_NUMBER_STRING_CHARACTERISTIC_UUID,
            SYSTEM_ID_STRING_CHARACTERISTIC_UUID,
            UDI_STRING_CHARACTERISTIC_UUID
        ))
        ServiceHandlerManager.instance?.addListener(this)
        addListener(DisInfoMap)
    }

    companion object {

        // Assigned GATT Service UUID Allocated for DIS
        val SERVICE_UUID: UUID = fromString("0000180a-0000-1000-8000-00805f9b34fb")

        val MANUFACTURER_NAME_STRING_CHARACTERISTIC_UUID: UUID =    fromString("00002A29-0000-1000-8000-00805f9b34fb")
        val MODEL_NUMBER_STRING_CHARACTERISTIC_UUID: UUID =         fromString("000002A24-0000-1000-8000-00805f9b34fb")
        val SERIAL_NUMBER_STRING_CHARACTERISTIC_UUID: UUID =        fromString("000002A25-0000-1000-8000-00805f9b34fb")
        val HARDWARE_REVISION_STRING_CHARACTERISTIC_UUID: UUID =    fromString("000002A27-0000-1000-8000-00805f9b34fb")
        val FIRMWARE_REVISION_STRING_CHARACTERISTIC_UUID: UUID =    fromString("000002A26-0000-1000-8000-00805f9b34fb")
        val SOFTWARE_REVISION_STRING_CHARACTERISTIC_UUID: UUID =    fromString("000002A28-0000-1000-8000-00805f9b34fb")
        val SYSTEM_ID_STRING_CHARACTERISTIC_UUID: UUID =            fromString("000002A23-0000-1000-8000-00805f9b34fb")
        val PNP_ID_STRING_CHARACTERISTIC_UUID: UUID =               fromString("000002A50-0000-1000-8000-00805f9b34fb")
        val UDI_STRING_CHARACTERISTIC_UUID: UUID =                  fromString("000007F3A-0000-1000-8000-00805f9b34fb")
    }
}