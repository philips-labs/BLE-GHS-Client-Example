/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import com.welie.blessed.WriteType
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
            Timber.i("SUCCESS: Writing <${value.asFormattedHexString()}> to <${characteristic.uuid}>")
        } else {
            Timber.i("ERROR: writing <${value.asFormattedHexString()}> to <${characteristic.uuid}> ($status)")
        }
    }

    open fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
    }

    open fun onDescriptorRead(
        peripheral: BluetoothPeripheral,
        value: ByteArray?,
        descriptor: BluetoothGattDescriptor,
        status: GattStatus
    ) {
    }

    open fun onDescriptorWrite(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
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

    protected fun enableAllNotifications(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        characteristics.filter {
            isCharacteristicSupported(it)
        }.forEach {
            enableNotify(peripheral, it)
        }
    }

    fun write(peripheral: BluetoothPeripheral, characteristicUUID: UUID, value: ByteArray) {
        peripheral.getCharacteristic(serviceUUID, characteristicUUID)?.let {
            val result = peripheral.writeCharacteristic(it, value, WriteType.WITH_RESPONSE)
            Timber.i( "Write of bytes: <${value.asHexString()}> for peripheral: $peripheral was $result")
        }
    }

    fun writeDescriptor(peripheral: BluetoothPeripheral, characteristicUUID: UUID, descriptorUUID: UUID, value: ByteArray) {
        peripheral.getCharacteristic(serviceUUID, characteristicUUID)?.let {
            val descriptor = it.getDescriptor(descriptorUUID)
            if (descriptor != null) {
                val result = peripheral.writeDescriptor(descriptor, value)
                Timber.i( "Set of descriptor uuid: $descriptorUUID from characteristic: $characteristicUUID on: ${peripheral.address} <${value.asHexString()}> for peripheral: $peripheral was $result")
            } else {
                Timber.i( "Get of descriptor uuid: $descriptorUUID from characteristic: $characteristicUUID on: ${peripheral.address} returned null")
            }
        }
    }

     fun enableNotify(
        peripheral: BluetoothPeripheral,
        characteristicUUID: UUID
    ) {
         val characteristic = peripheral.getCharacteristic(serviceUUID, characteristicUUID)
         if (characteristic != null) {
            enableNotify(peripheral, characteristic)
         } else {
             val message =
                 "Peripheral ${peripheral.name} setNotify failed for missing ${characteristicUUID}"
             throw ServiceHandlerCharacteristicException(message)
         }
    }
    fun enableNotify(
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

    private val CCC_DESCRIPTOR_UUID: UUID? = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val enableIndications = byteArrayOf(2.toByte(), 0.toByte())

    fun enableIndicate(
        peripheral: BluetoothPeripheral,
        characteristicUUID: UUID
    ) {
        val characteristic = peripheral.getCharacteristic(serviceUUID, characteristicUUID)
        if (characteristic != null) {
            if (characteristic.isIndicate()) {
                if (!peripheral.writeDescriptor(characteristic.getDescriptor(CCC_DESCRIPTOR_UUID), enableIndications)) {
                    val message =
                        "Peripheral ${peripheral.name} enableIndicate failed for ${characteristic.uuid}"
                    throw ServiceHandlerCharacteristicException(message)
                }
            } else {
                val message = "Indications cannot be enabled for ${characteristic.uuid}"
                throw ServiceHandlerCharacteristicException(message)
            }
        }
    }

    // Private methods

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