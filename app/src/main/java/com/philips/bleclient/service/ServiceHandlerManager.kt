/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.philips.bleclient.service.dis.DisServiceHandler
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.sts.SimpleTimeServiceHandler
import com.philips.bleclient.service.user.UserDataServiceHandler
import com.philips.bleclient.service.user.UserDataServiceHandlerListener
import com.philips.bleclient.ui.ObservationLog.log
import com.philips.bleclient.ui.RacpLog
import com.philips.bleclient.ui.isBonded
import com.welie.blessed.*
import timber.log.Timber
import java.util.*

interface ServiceHandlerManagerListener {
    fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult)
    fun onConnectedPeripheral(peripheral: BluetoothPeripheral)
    fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
    }
}

class ServiceHandlerManager private constructor(context: Context) {

    private fun racpLog(message: String) {
        RacpLog.log(message)
        Timber.i(message)
    }

    private val  GATT_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    private val GAP_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
    private val RCS_UUID = UUID.fromString("00001829-0000-1000-8000-00805f9b34fb")

    public fun serviceUUIDtoString(uuid: UUID) : String {
        return when(uuid){
            GenericHealthSensorServiceHandler.SERVICE_UUID -> "GHSS"
            SimpleTimeServiceHandler.SERVICE_UUID -> "ETS"
            UserDataServiceHandler.SERVICE_UUID -> "UDS"
            GATT_UUID -> "GATT"
            GAP_UUID -> "GAP"
            DisServiceHandler.SERVICE_UUID -> "DIS"
            RCS_UUID -> "RCS"
            else -> uuid.toString()
        }
    }

    var central: BluetoothCentralManager
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredPeripherals = mutableSetOf<BluetoothPeripheral>()
    private val serviceHandlers = HashMap<UUID, ServiceHandler>()
    private val listeners = mutableSetOf<ServiceHandlerManagerListener>()
    private val peripheralCallback: BluetoothPeripheralCallback =
        object : BluetoothPeripheralCallback() {

            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                peripheral.services.forEach {
                        service ->
                    racpLog("Service found: " + serviceUUIDtoString(service.uuid))
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

            override fun onDescriptorRead(
                peripheral: BluetoothPeripheral,
                value: ByteArray,
                descriptor: BluetoothGattDescriptor,
                status: GattStatus
            ) {
                serviceHandlers[descriptor.characteristic.service.uuid]?.onDescriptorRead(
                    peripheral,
                    value,
                    descriptor,
                    status)
            }

            override fun onDescriptorWrite(
                peripheral: BluetoothPeripheral,
                value: ByteArray,
                descriptor: BluetoothGattDescriptor,
                status: GattStatus
            ) {
                serviceHandlers[descriptor.characteristic.service.uuid]?.onDescriptorWrite(
                    peripheral,
                    value,
                    descriptor,
                    status)
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
                    Timber.i("Found peripheral ${peripheral.name} scan record: ${scanResult.scanRecord}", )
                    listeners.forEach { it.onDiscoveredPeripheral(peripheral, scanResult) }
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
        if( !peripheral.isBonded()) {
            central.createBond(peripheral, peripheralCallback)
        } else {
            Timber.i("Already bonded with: %s", peripheral.name)
            central.connectPeripheral(peripheral, peripheralCallback)
        }
    }

    fun unbond(peripheral: BluetoothPeripheral) {
        if(!(peripheral.bondState == BondState.NONE)) unbond(peripheral.address)
    }

    fun unbond(peripheralAddress: String) {
        central.removeBond(peripheralAddress)
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
        Timber.i("Service handler added for:" + serviceUUIDtoString(serviceHandler.serviceUUID))
    }

    fun serviceHandlerForUUID(serviceUUID: UUID): ServiceHandler? {
        return serviceHandlers[serviceUUID]
    }

    companion object {
        @Volatile
        internal var instance: ServiceHandlerManager? = null

        fun getInstance(): ServiceHandlerManager? { return instance }

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
