package com.philips.bleclient.service.dis

import com.welie.blessed.BluetoothPeripheral

object DisInfoMap : DisServiceListener {

    val deviceInfoMap = mutableMapOf<String, MutableMap<DisInfoItem, Any>>()

    fun getInfo(address: String): String {
        return DisInfoItem.values().fold("Device information:\n", { string, item -> string + getDeviceInfoValue(address, item)})
    }

    fun formatLine(item: DisInfoItem, v: String): String {
        return if (v.isEmpty()) "" else "${item.name}: $v\n"
    }

    fun getDeviceInfoValue(deviceAddress: String, item: DisInfoItem): String {
        return formatLine(item, deviceInfoMap.get(deviceAddress)?.get(item)?.let { it as String } ?: "")
    }

    override fun onDisconnected(address: String) {
        deviceInfoMap.remove(address)
    }

    override fun onConnected(address: String) {
        // nothing to do yet
    }

    fun setDeviceInfoValue(peripheral: BluetoothPeripheral, item: DisInfoItem, itemValue: String){
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(item, itemValue)
    }

}