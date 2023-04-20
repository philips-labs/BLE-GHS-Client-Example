package com.philips.bleclient.service.bas

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.ServiceHandlerManagerListener
import com.philips.bleclient.asHexString
import com.philips.bleclient.service.dis.DisInfoMap
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

class BasServiceHandler : ServiceHandler(), ServiceHandlerManagerListener {

    private val listeners = mutableListOf<BasServiceListener>()
    private val peripherals = mutableListOf<BluetoothPeripheral>()
    private var batteryLevels = mutableMapOf<String, Int>()

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
        peripheral.readCharacteristic(SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID)
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        //super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        Timber.i("Received BAS characteristic value: ${characteristic.uuid} - value: ${value.asHexString()}")
        if (status == GattStatus.SUCCESS) {
            when (characteristic.uuid) {
                BATTERY_LEVEL_CHARACTERISTIC_UUID -> {
                    handleBatteryLevel(peripheral, value)
                }
                else -> {
                    Timber.i("BAS Unexpected characteristic update received UUID:${characteristic.uuid}, value: ${value.asHexString()}")
                }

            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }


    private fun handleBatteryLevel(device: BluetoothPeripheral, value: ByteArray) {
        val parser = BluetoothBytesParser(value)
        val batteryLevel = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        batteryLevels.put(device.address, batteryLevel)
        Timber.i("Received battery level $batteryLevel for ${device.name}")
        // post to listeners
        for (l in listeners){
            l.onBatteryLevel(device.address, batteryLevel)
        }
    }

    companion object {
        private val TAG = BasServiceHandler::class.simpleName.toString()
        val SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHARACTERISTIC_UUID =
            UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.add(BATTERY_LEVEL_CHARACTERISTIC_UUID)
        ServiceHandlerManager.instance?.addListener(this)
        listeners.add(DisInfoMap)
    }

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        // nothing to do here
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("BAS Service Handler: Connected Peripheral ${peripheral.address}")
        if (!peripherals.contains(peripheral)) {
            peripherals.add(peripheral)
        }
        listeners.forEach { it.onConnected(peripheral.address) }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        super.onDisconnectedPeripheral(peripheral)
        Timber.i("BAS Service Handler: Disconnected Peripheral ${peripheral.address}")
        peripherals.remove(peripheral)
        listeners.forEach { it.onDisconnected(peripheral.address) }
    }

}