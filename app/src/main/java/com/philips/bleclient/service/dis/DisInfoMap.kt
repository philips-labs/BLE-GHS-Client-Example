package com.philips.bleclient.service.dis

import com.philips.bleclient.ui.GHSDeviceInfoMap
import com.welie.blessed.BluetoothPeripheral
import okhttp3.internal.format

object DisInfoMap : DisServiceListener {
    val MAN_NAME : String = "Manufacturer"
    val MODEL_NUMBER : String = "Model"
    val UDI : String = "UDI"
    val SERIAL : String = "Serial"
    val SYSID = "SysID"
    val newLine = "\n";

    val deviceInfoMap = mutableMapOf<String, MutableMap<String, Any>>()

    fun getInfo(address: String): String {
        return "Device information:" + newLine +
                getManufacturerName(address) +
                getModelNumber(address) +
                getSysID(address) +
                getSerial(address) +
                getUDI(address)
    }

    fun formatLine(item: String, v: String?): String {
        return if (v != null){
            "$item:$v$newLine"
        } else ""
    }

    fun getManufacturerName(deviceAddress: String): String? {
        return formatLine(MAN_NAME, deviceInfoMap.get(deviceAddress)?.get(MAN_NAME)?.let { it as String })
    }

    fun getModelNumber(deviceAddress: String): String? {
        return formatLine(MODEL_NUMBER, deviceInfoMap.get(deviceAddress)?.get(MODEL_NUMBER)?.let { it as String })
    }

    fun getUDI(deviceAddress: String): String? {
        return formatLine(UDI, deviceInfoMap.get(deviceAddress)?.get(UDI)?.let { it as String })
    }

    fun getSerial(deviceAddress: String): String? {
        return formatLine(SERIAL, deviceInfoMap.get(deviceAddress)?.get(SERIAL)?.let { it as String })
    }

    fun getSysID(address: String): String? {
        return formatLine(SYSID, deviceInfoMap.get(address)?.get(SYSID)?.let { it as String })
    }

    override fun onDisconnected(deviceAddress: String) {
        deviceInfoMap.remove(deviceAddress)
    }

    override fun onConnected(address: String) {
        // nothing to do yet
    }

    fun setManufacturerName(peripheral: BluetoothPeripheral, name: String) {
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(MAN_NAME, name)
    }

    fun setModelNumber(peripheral: BluetoothPeripheral, name: String) {
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(MODEL_NUMBER, name)
    }

    fun setUDI(peripheral: BluetoothPeripheral, name: String) {
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(UDI, name)
    }

    fun setSerialNumber(peripheral: BluetoothPeripheral, name: String) {
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(SERIAL, name)
    }

    fun setSystemId(peripheral: BluetoothPeripheral, toString: String) {
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(SYSID, toString)
    }

}