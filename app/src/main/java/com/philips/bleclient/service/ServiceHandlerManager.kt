/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.welie.blessed.*
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

interface ServiceHandlerManagerListener {
    fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral)
    fun onConnectedPeripheral(peripheral: BluetoothPeripheral)
    fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
    }
}

class ServiceHandlerManager private constructor(context: Context) {
    var central: BluetoothCentralManager
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredPeripherals = mutableSetOf<BluetoothPeripheral>()
    private val serviceHandlers = HashMap<UUID, ServiceHandler>()
    private val listeners = mutableSetOf<ServiceHandlerManagerListener>()
    private val peripheralCallback: BluetoothPeripheralCallback =
        object : BluetoothPeripheralCallback() {

            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                peripheral.services.forEach { service ->
                    serviceHandlers[service.uuid]?.onCharacteristicsDiscovered(
                        peripheral,
                        service.characteristics
                    )
                }
            }

            override fun onNotificationStateUpdate(
                peripheral: BluetoothPeripheral,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                serviceHandlers[characteristic.service.uuid]?.onNotificationStateUpdate(
                    peripheral,
                    characteristic,
                    status
                )
            }

            override fun onCharacteristicWrite(
                peripheral: BluetoothPeripheral,
                value: ByteArray,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                serviceHandlers[characteristic.service.uuid]?.onCharacteristicWrite(
                    peripheral,
                    value,
                    characteristic,
                    status
                )
            }

            override fun onCharacteristicUpdate(
                peripheral: BluetoothPeripheral,
                value: ByteArray,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                serviceHandlers[characteristic.service.uuid]?.onCharacteristicUpdate(
                    peripheral,
                    value,
                    characteristic,
                    status
                )
            }
        }

    private val bluetoothCentralManagerCallback: BluetoothCentralManagerCallback =
        object : BluetoothCentralManagerCallback() {
            override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
                listeners.forEach { it.onConnectedPeripheral(peripheral) }
                Timber.i("connected to '%s'", peripheral.name)
            }

            override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
                Timber.e("connection '%s' failed with status %s", peripheral.name, status)
            }

            override fun onDisconnectedPeripheral(
                peripheral: BluetoothPeripheral,
                status: HciStatus
            ) {
                Timber.i("disconnected '%s' with status %s", peripheral.name, status)
                listeners.forEach { it.onDisconnectedPeripheral(peripheral) }
            }

            override fun onDiscoveredPeripheral(
                peripheral: BluetoothPeripheral,
                scanResult: ScanResult
            ) {

                if (discoveredPeripherals.add(peripheral)) {
                    Timber.i("Found peripheral '%s'", peripheral.name)
                    listeners.forEach { it.onDiscoveredPeripheral(peripheral) }
                }
            }

            override fun onBluetoothAdapterStateChanged(state: Int) {
                Timber.i("bluetooth adapter changed state to %d", state)
                if (state == BluetoothAdapter.STATE_ON) {
                    startScanning()
                }
            }

            override fun onScanFailed(scanFailure: ScanFailure) {
                Timber.i("scanning failed with error %s", scanFailure)
            }
        }

    fun connect(peripheral: BluetoothPeripheral) {
        central.connectPeripheral(peripheral, peripheralCallback)
    }

    fun bond(peripheral: BluetoothPeripheral) {
        central.createBond(peripheral, peripheralCallback)
    }

    fun addListener(listener: ServiceHandlerManagerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ServiceHandlerManagerListener) {
        listeners.remove(listener)
    }

    fun startScanning() {
        central.startPairingPopupHack()
        handler.postDelayed(
            { central.scanForPeripheralsWithServices(getConnectServiceUUIDs()) },
            1000
        )
    }

    fun stopScanning() {
        central.stopScan()
    }

    fun isScanning(): Boolean {
        return central.isScanning
    }

    fun getConnectedPeripherals(): List<BluetoothPeripheral> {
        return central.connectedPeripherals
    }

    fun getConnectedPeripheral(peripheralAddress: String): BluetoothPeripheral? {
        return central.connectedPeripherals.firstOrNull {
            it.address.equals(
                peripheralAddress,
                true
            )
        }
    }

    private fun getConnectServiceUUIDs(): Array<UUID> {
        return serviceHandlers.values.map { it.serviceUUID }.toTypedArray()
    }

    fun addServiceHandler(serviceHandler: ServiceHandler) {
        serviceHandlers[serviceHandler.serviceUUID] = serviceHandler
    }

    fun serviceHandlerForUUID(serviceUUID: UUID): ServiceHandler? {
        return serviceHandlers[serviceUUID]
    }

    companion object {
        @Volatile
        internal var instance: ServiceHandlerManager? = null

        fun getInstance(context: Context): ServiceHandlerManager {
            return instance ?: synchronized(this) {
                ServiceHandlerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        central = BluetoothCentralManager(
            context,
            bluetoothCentralManagerCallback,
            Handler(Looper.getMainLooper())
        )
    }
}
