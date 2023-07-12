/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */

package com.philips.bleclient.service.ets

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import com.philips.bleclient.*
import com.philips.bleclient.extensions.*
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

class ElapsedTimeServiceHandler : ServiceHandler(),
    ServiceHandlerManagerListener {

    var listeners: MutableSet<ElapsedTimeServiceHandlerListener> = mutableSetOf()
    private val peripherals = mutableSetOf<BluetoothPeripheral>()
    private val peripheralETSFlags = mutableMapOf<BluetoothPeripheral, BitMask>()

    internal val elapsedTimeCharacteristic = BluetoothGattCharacteristic(
        ELAPSED_TIME_CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_INDICATE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    override val name: String
        get() = "ElapsedTimeServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        // enableAllNotificationsAndRead(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
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
                ELAPSED_TIME_CHARACTERISTIC_UUID -> handleTimeBytes(
                    peripheral,
                    value
                )
            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    /*
     * ElapsedTimeServiceHandler Listener methods (add/remove)
     */

    fun addListener(listener: ElapsedTimeServiceHandlerListener) = listeners.add(listener)

    fun removeListener(listener: ElapsedTimeServiceHandlerListener) = listeners.remove(listener)

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.add(peripheral)
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.remove(peripheral)
        peripheralETSFlags.remove(peripheral)
    }

    private fun handleTimeBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Time Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        peripheralETSFlags.put(peripheral, BitMask(value.first()))
        listeners.forEach { it.onReceivedEtsBytes(peripheral.address, value) }
    }

    fun getETSBytes(peripheral: BluetoothPeripheral) {
        peripheral.readCharacteristic(SERVICE_UUID, ELAPSED_TIME_CHARACTERISTIC_UUID)
    }

    fun setETSBytes(peripheral: BluetoothPeripheral) {
        peripheralETSFlags.get(peripheral)?.let {
            if (it.hasFlag(TimestampFlags.isTickCounter)) {
                resetTickCounter(peripheral)
            } else {
                setServerTime(peripheral, it)
            }
        } ?: Timber.i("No peripheralETSFlags for peripheral ${peripheral.address}")
    }


//    // For now set the time based on the time flags we have, vs. what we got from the peripheral
//    fun setETSBytes(peripheral: BluetoothPeripheral) {
//        if (TimestampFlags.currentFlags.hasFlag(TimestampFlags.isTickCounter)) {
//            resetTickCounter(peripheral)
//        } else {
//            write(peripheral, ELAPSED_TIME_CHARACTERISTIC_UUID, Date().asGHSBytes())
//        }
//    }

    fun resetTickCounter(peripheral: BluetoothPeripheral) {
        peripheralETSFlags.get(peripheral)?.let {
            if (it.hasFlag(TimestampFlags.isTickCounter)) {
                resetETSTicks(peripheral)
            } else {
                Timber.i("Not a tick counter, cannot reset")
            }
        } ?: Timber.i("No peripheralETSFlags for peripheral ${peripheral.address}")
    }

    fun setServerTime(peripheral: BluetoothPeripheral, flags: BitMask) {
        peripheralETSFlags.get(peripheral)?.let {
            write(peripheral, ELAPSED_TIME_CHARACTERISTIC_UUID, Date().asGHSBytes(it))
        }
    }

    fun getClientTimeAsGHSBytes(peripheral: BluetoothPeripheral?) : ByteArray {
        var flags = peripheralETSFlags.get(peripheral)
        if (flags != null) {
            return Date().asGHSBytes(flags)
        } else {
            return Date().asGHSBytes()
        }
    }


    fun resetETSTicks(peripheral: BluetoothPeripheral) {
        peripheralETSFlags.get(peripheral)?.let {
            write(peripheral, ELAPSED_TIME_CHARACTERISTIC_UUID, 0L.asGHSTicks(it))
        }
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.add(ELAPSED_TIME_CHARACTERISTIC_UUID)
        ServiceHandlerManager.instance?.addListener(this)
    }

    companion object {
        val SERVICE_UUID = UUID.fromString("0000183f-0000-1000-8000-00805f9b34fb")

        val ELAPSED_TIME_CHARACTERISTIC_UUID =
            UUID.fromString("00002bf2-0000-1000-8000-00805f9b34fb")

    }
}