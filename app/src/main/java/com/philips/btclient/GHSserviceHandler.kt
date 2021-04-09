package com.philips.btclient

import android.bluetooth.BluetoothGattCharacteristic
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import java.util.*

class GHSserviceHandler: ServiceHandler() {

    override fun onCharacteristicsDiscovered(peripheral: BluetoothPeripheral, characteristics: List<BluetoothGattCharacteristic>) {
        super.onCharacteristicsDiscovered(peripheral, characteristics)
    }


    override fun onCharacteristicWrite(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        super.onCharacteristicWrite(peripheral, value, characteristic, status)
    }

    override fun onNotificationStateUpdate(peripheral: BluetoothPeripheral, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        super.onNotificationStateUpdate(peripheral, characteristic, status)
    }

    override fun onCharacteristicUpdate(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
    }

    init {
        serviceUUID = SERVICE_UUID
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("2893b28b-c868-423a-9dc2-e9c2fcb4ebb5")
        private val KINSA_AUTOLAUNCH_SERVICE_UUID: UUID = UUID.fromString("3893b28b-c868-423a-9dc2-e9c2fcb4ebb5")
    }
}