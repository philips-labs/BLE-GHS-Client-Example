/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient

import android.bluetooth.BluetoothGattCharacteristic
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

class ServiceHandlerCharacteristicException(message: String) : Exception(message)

open class ServiceHandler {

    open val name: String
        get() = "ServiceHandler"
    lateinit var serviceUUID: UUID
    protected var supportedCharacteristics: MutableSet<UUID> = LinkedHashSet()

    open fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {

    }

    open fun onNotificationStateUpdate(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        if (status == GattStatus.SUCCESS) {
            val isNotifying = peripheral.isNotifying(characteristic)
            Timber.i("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.uuid)
        } else {
            Timber.e(
                "ERROR: Changing notification state failed for %s (%s)", characteristic.uuid, status
            )
        }
    }

    open fun onCharacteristicWrite(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        if (status == GattStatus.SUCCESS) {
            Timber.i(
                "SUCCESS: Writing <%s> to <%s>",
                BluetoothBytesParser.bytes2String(value),
                characteristic.uuid
            )
        } else {
            Timber.i(
                "ERROR: Failed writing <%s> to <%s> (%s)",
                BluetoothBytesParser.bytes2String(value),
                characteristic.uuid,
                status
            )
        }
    }

    open fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
    }

    open fun isCharacteristicSupported(characteristic: BluetoothGattCharacteristic): Boolean {
        return supportedCharacteristics.contains(characteristic.uuid)
    }

    // Protected methods

    protected fun enableAllNotificationsAndRead(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        characteristics.filter {
            isCharacteristicSupported(it)
        }.forEach {
            checkAndReadCharacteristic(peripheral, it)
            enableNotify(peripheral, it)
        }
    }

    // Private methods

    private fun enableNotify(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (characteristic.isNotify() || characteristic.isIndicate()) {
            if (!peripheral.setNotify(characteristic, true)) {
                val message =
                    "Peripheral ${peripheral.name} setNotify failed for ${characteristic.uuid}"
                throw ServiceHandlerCharacteristicException(message)
            }
        }
    }

    private fun checkAndReadCharacteristic(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (characteristic.isRead()) {
            if (!peripheral.readCharacteristic(characteristic)) {
                val message =
                    "Peripheral ${peripheral.name} readCharacteristic failed for ${characteristic.uuid}"
                throw ServiceHandlerCharacteristicException(message)
            }
        }
    }

}