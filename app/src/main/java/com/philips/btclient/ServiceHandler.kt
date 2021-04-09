package com.philips.btclient

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

open class ServiceHandler {

    lateinit var serviceUUID : UUID

    open fun onCharacteristicsDiscovered(peripheral: BluetoothPeripheral, characteristics: List<BluetoothGattCharacteristic>) {

    }

    open fun onNotificationStateUpdate(peripheral: BluetoothPeripheral, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        if (status == GattStatus.SUCCESS) {
            val isNotifying = peripheral.isNotifying(characteristic)
            Timber.i("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.uuid)
        } else {
            Timber.e(
                "ERROR: Changing notification state failed for %s (%s)", characteristic.uuid, status
            )
        }
    }

    open fun onCharacteristicWrite(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
        if (status == GattStatus.SUCCESS) {
            Timber.i("SUCCESS: Writing <%s> to <%s>", BluetoothBytesParser.bytes2String(value), characteristic.uuid)
        } else {
            Timber.i("ERROR: Failed writing <%s> to <%s> (%s)", BluetoothBytesParser.bytes2String(value), characteristic.uuid, status)
        }
    }

    open fun onCharacteristicUpdate(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {}
}