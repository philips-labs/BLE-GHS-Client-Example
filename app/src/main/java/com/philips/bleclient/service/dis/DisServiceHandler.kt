package com.philips.bleclient.service.dis

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.ServiceHandlerManagerListener
import com.philips.bleclient.extensions.BitMask
import com.philips.bleclient.extensions.Flags
import com.philips.bleclient.extensions.hasFlag
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothBytesParser.FORMAT_UINT64
import com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.nio.ByteOrder
import java.util.*
import java.util.UUID.*


class DisServiceHandler : ServiceHandler(), ServiceHandlerManagerListener {

    private val listeners = mutableListOf<DisServiceListener>()
    private val peripherals = mutableListOf<BluetoothPeripheral>()

    private val stringInfoItem = setOf<DisInfoItem>(DisInfoItem.MANUFACTURER_NAME, DisInfoItem.MODEL_NUMBER, DisInfoItem.SERIAL_NUMBER, DisInfoItem.SOFTWARE_REVISION, DisInfoItem.HARDWARE_REVISION, DisInfoItem.FIRMWARE_REVISION)

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        readDeviceInformation(peripheral)
    }

    enum class UDIFlags(override val bit: Long) : Flags {
        label((1 shl 0).toLong()),
        deviceIdentifier((1 shl 1).toLong()),
        issuer((1 shl 2).toLong()),
        authority((1 shl 3).toLong())
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
        if (status == GattStatus.SUCCESS) {
            for (disItem in stringInfoItem) {
                if (characteristic.uuid == disItem.value){
                    val itemValue = parser.getStringValue();
                    Timber.i(disItem.name + ":" + itemValue)
                    DisInfoMap.setDeviceInfoValue(peripheral, disItem, itemValue)
                    return
                }
            }
            when (characteristic.uuid) {
                DisInfoItem.SYSTEM_ID.value -> {
                    val sysId = parser.getLongValue(FORMAT_UINT64);
                    Timber.i("System ID read:" + sysId)
                    DisInfoMap.setDeviceInfoValue(peripheral, DisInfoItem.SYSTEM_ID, sysId.toString())
                }
                DisInfoItem.UDI.value -> {
                    val flags : Long = parser.getByteArray(1)[0].toLong()
                    val udiBytes = parser.getByteArray(value.size - 1);
                    Timber.i("UDI read:" + String(udiBytes))
                    var prevIndex : Int = 0
                    val udiElements = mutableListOf<String>()
                    for(i in 1..(udiBytes.size-1) ){
                        if (udiBytes[i] == 0.toByte()) {
                            udiElements.add(String(udiBytes.copyOfRange(prevIndex,i), Charsets.UTF_8))
                            prevIndex = i+1
                        }
                    }
                    var udiString : String = ""
                    var index = 0
                    for( flag in UDIFlags.values()){
                        if (flags and flag.bit != 0L) {
                            udiString = udiString + flag.name + ": " + udiElements[index] + " "
                            Timber.i(flag.name + ": " + udiElements[index])
                            index++
                        }
                    }
                    //udi.replace(0.toChar(),'\n')
                    DisInfoMap.setDeviceInfoValue(peripheral, DisInfoItem.UDI, udiString + "\n")
                }
                else -> Timber.i("Unknown DIS characteristic read - UUID:" + characteristic.uuid + " value:" + value.toString())
            }
        } else {
            Timber.i("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    fun readDeviceInformation(peripheral: BluetoothPeripheral){
        for(disItem in DisInfoItem.values()){
            val characteristic = peripheral.getCharacteristic(SERVICE_UUID, disItem.value)
            if (characteristic != null) {
                timber.log.Timber.i( "Reading ${disItem.name}" )
                if (!peripheral.readCharacteristic(characteristic)){
                    timber.log.Timber.i("Read failed!")
                }
            } else {
                // timber.log.Timber.i("No need to read ${disItem.name}")
            }
        }
    }

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        // nothing to do here
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        Timber.i("DIS Service Handler: Connected Peripheral ${peripheral.address}")
        if (!peripherals.contains(peripheral)) {
            peripherals.add(peripheral)
        }
        listeners.forEach { it.onConnected(peripheral.address) }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        super.onDisconnectedPeripheral(peripheral)
        Timber.i("DIS Service Handler: Disconnected Peripheral ${peripheral.address}")
        peripherals.remove(peripheral)
        listeners.forEach { it.onDisconnected(peripheral.address) }
    }

    fun addListener(listener: DisServiceListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(DisInfoItem.values().map { it.value })
        ServiceHandlerManager.instance?.addListener(this)
        //addListener(DisInfoMap)
    }

    companion object {

        // Assigned GATT Service UUID Allocated for DIS
        val SERVICE_UUID: UUID = fromString("0000180a-0000-1000-8000-00805f9b34fb")

    }
}