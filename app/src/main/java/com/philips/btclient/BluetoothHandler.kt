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

class BluetoothHandler private constructor(context: Context) {
    lateinit var central: BluetoothCentralManager
    private val handler = Handler(Looper.getMainLooper())
    private val serviceHandlers = HashMap<UUID, ServiceHandler>()
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

                // Reconnect to this device when it becomes available again
                handler.postDelayed({
                    central.autoConnectPeripheral(peripheral, peripheralCallback)
                }, 5000)
            }

            override fun onDiscoveredPeripheral(
                peripheral: BluetoothPeripheral,
                scanResult: ScanResult
            ) {
                Timber.i("Found peripheral '%s'", peripheral.name)
                central.stopScan()
                central.connectPeripheral(peripheral, peripheralCallback)
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

    companion object {

        private var instance: BluetoothHandler? = null

        @Synchronized
        fun getInstance(context: Context): BluetoothHandler {
            if (instance == null) {
                instance = BluetoothHandler(context.applicationContext)
            }
            return requireNotNull(instance)
        }
    }

    init {
        Timber.plant(DebugTree())

        central = BluetoothCentralManager(
            context,
            bluetoothCentralManagerCallback,
            Handler(Looper.getMainLooper())
        )

        val ghsServiceHandler = GenericHealthSensorServiceHandler()
        serviceHandlers[ghsServiceHandler.serviceUUID] = ghsServiceHandler
        startScanning()
    }

    fun startScanning() {
        // Scan for peripherals with a certain service UUIDs
        central.startPairingPopupHack()
        handler.postDelayed(
            { central.scanForPeripheralsWithServices(arrayOf(GenericHealthSensorServiceHandler.SERVICE_UUID)) },
            1000
        )
    }
}