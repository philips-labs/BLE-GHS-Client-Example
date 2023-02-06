/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */

package com.philips.bleclient.service.sts

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import com.philips.bleclient.*
import com.philips.bleclient.extensions.*
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import com.welie.blessed.WriteType
import timber.log.Timber
import java.util.*

class SimpleTimeServiceHandler : ServiceHandler(),
    ServiceHandlerManagerListener {

    var listeners: MutableSet<SimpleTimeServiceHandlerListener> = mutableSetOf()
    private val peripherals = mutableSetOf<BluetoothPeripheral>()
    private val peripheralSTSFlags = mutableMapOf<BluetoothPeripheral, BitMask>()

    internal val simpleTimeCharacteristic = BluetoothGattCharacteristic(
        SIMPLE_TIME_CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_INDICATE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    override val name: String
        get() = "SimpleTimeServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        // enableAllNotificationsAndRead(peripheral, characteristics)
        // enableAllNotifications(peripheral, characteristics)
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
                SIMPLE_TIME_CHARACTERISTIC_UUID -> handleTimeBytes(
                    peripheral,
                    value
                )
            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    /*
     * SimpleTimeServiceHandler Listener methods (add/remove)
     */

    fun addListener(listener: SimpleTimeServiceHandlerListener) = listeners.add(listener)

    fun removeListener(listener: SimpleTimeServiceHandlerListener) = listeners.remove(listener)

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.add(peripheral)
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.remove(peripheral)
        peripheralSTSFlags.remove(peripheral)
    }

    private fun handleTimeBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Time Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        peripheralSTSFlags.put(peripheral, value.first().asBitmask())
        listeners.forEach { it.onReceivedStsBytes(peripheral.address, value) }
    }

    fun getSTSBytes(peripheral: BluetoothPeripheral) {
        peripheral.readCharacteristic(SERVICE_UUID, SIMPLE_TIME_CHARACTERISTIC_UUID)
    }

    fun setSTSBytes(peripheral: BluetoothPeripheral) {
        peripheralSTSFlags.get(peripheral)?.let {
            if (it.hasFlag(TimestampFlags.isTickCounter)) {
                resetTickCounter(peripheral)
            } else {
                setServerTime(peripheral, it)
            }
        } ?: Timber.i("No peripheralSTSFlags for peripheral ${peripheral.address}")
    }


//    // For now set the time based on the time flags we have, vs. what we got from the peripheral
//    fun setSTSBytes(peripheral: BluetoothPeripheral) {
//        if (TimestampFlags.currentFlags.hasFlag(TimestampFlags.isTickCounter)) {
//            resetTickCounter(peripheral)
//        } else {
//            write(peripheral, SIMPLE_TIME_CHARACTERISTIC_UUID, Date().asGHSBytes())
//        }
//    }

    fun resetTickCounter(peripheral: BluetoothPeripheral) {
        peripheralSTSFlags.get(peripheral)?.let {
            if (it.hasFlag(TimestampFlags.isTickCounter)) {
                resetSTSTicks(peripheral)
            } else {
                Timber.i("Not a tick counter, cannot reset")
            }
        } ?: Timber.i("No peripheralSTSFlags for peripheral ${peripheral.address}")
    }

    fun setServerTime(peripheral: BluetoothPeripheral, flags: BitMask) {
        peripheralSTSFlags.get(peripheral)?.let {
            write(peripheral, SIMPLE_TIME_CHARACTERISTIC_UUID, Date().asGHSBytes(it))
        }
    }

    fun resetSTSTicks(peripheral: BluetoothPeripheral) {
        peripheralSTSFlags.get(peripheral)?.let {
            write(peripheral, SIMPLE_TIME_CHARACTERISTIC_UUID, 0L.asGHSTicks(it))
        }
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.add(SIMPLE_TIME_CHARACTERISTIC_UUID)
        ServiceHandlerManager.instance?.addListener(this)
    }

    companion object {
        val SERVICE_UUID = UUID.fromString("00007f3E-0000-1000-8000-00805f9b34fb")

        val SIMPLE_TIME_CHARACTERISTIC_UUID =
            UUID.fromString("00007f3d-0000-1000-8000-00805f9b34fb")

    }
}