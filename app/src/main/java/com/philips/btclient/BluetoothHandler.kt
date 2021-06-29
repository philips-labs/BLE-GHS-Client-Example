package com.philips.btclient

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.philips.btclient.ghs.GenericHealthSensorServiceHandler
import com.welie.blessed.*
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.*
import kotlin.collections.HashMap

interface BluetoothHandlerListener {
    fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral)
    fun onConnectedPeripheral(peripheral: BluetoothPeripheral)
    fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
    }
}

class BluetoothHandler private constructor(context: Context) {
    lateinit var central: BluetoothCentralManager
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredPeripherals = mutableSetOf<BluetoothPeripheral>()
    private val serviceHandlers = HashMap<UUID, ServiceHandler>()
    private val listeners = mutableSetOf<BluetoothHandlerListener>()
    private val peripheralCallback: BluetoothPeripheralCallback =
        object : BluetoothPeripheralCallback() {

            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                //peripheral.requestConnectionPriority(CONNECTION_PRIORITY_HIGH)
                val services = peripheral.services
                for (service in services) {
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
                serviceHandlers[characteristic.service.uuid]?.onCharacteristicUpdate(
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

    // Callback for central
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

//                // Reconnect to this device when it becomes available again
//                handler.postDelayed({
//                    central.autoConnectPeripheral(peripheral, peripheralCallback)
//                }, 5000)
            }

            override fun onDiscoveredPeripheral(
                peripheral: BluetoothPeripheral,
                scanResult: ScanResult
            ) {

                if (discoveredPeripherals.add(peripheral)) {
                    Timber.i("Found peripheral '%s'", peripheral.name)
                    listeners.forEach { it.onDiscoveredPeripheral(peripheral) }
                }
//                central.stopScan()
//                central.connectPeripheral(peripheral, peripheralCallback)
            }

            override fun onBluetoothAdapterStateChanged(state: Int) {
                Timber.i("bluetooth adapter changed state to %d", state)
                if (state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is on now, start scanning again
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

    fun addListener(listener: BluetoothHandlerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BluetoothHandlerListener) {
        listeners.remove(listener)
    }

    fun startScanning() {
        // Scan for peripherals with a certain service UUIDs
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

    fun getPeripheral(peripheralAddress: String): BluetoothPeripheral? {
        return central.getPeripheral(peripheralAddress)
    }

    fun getConnectedPeripherals(): List<BluetoothPeripheral> {
        return central.connectedPeripherals
    }

    fun getConnectedPeripheral(peripheralAddress: String): BluetoothPeripheral? {
        return central.connectedPeripherals.firstOrNull { it.address.equals(peripheralAddress, true) }
    }

    fun getConnectServiceUUIDs(): Array<UUID> {
        return serviceHandlers.values.map { it.serviceUUID }.toTypedArray()
    }

    fun addServiceHander(serviceHandler: ServiceHandler) {
        serviceHandlers[serviceHandler.serviceUUID] = serviceHandler
    }

    companion object {
        @Volatile private var instance: BluetoothHandler? = null

        fun getInstance(context: Context): BluetoothHandler {
            return instance ?: synchronized(this) {
                BluetoothHandler(context.applicationContext).also { instance = it }
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