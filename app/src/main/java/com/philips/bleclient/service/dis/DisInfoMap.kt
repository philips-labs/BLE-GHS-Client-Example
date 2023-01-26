package com.philips.bleclient.service.dis

import com.philips.bleclient.ui.GHSDeviceInfoMap
import com.welie.blessed.BluetoothPeripheral
import okhttp3.internal.format

object DisInfoMap : DisServiceListener {

    val deviceInfoMap = mutableMapOf<String, MutableMap<DisInfoItem, Any>>()
    val newLine = "\n";

    fun getInfo(address: String): String {
        var info : String = "Device information:" + newLine
        for (item in DisInfoItem.values()) info += getDeviceInfoValue(address, item)
        return info
    }

    fun formatLine(item: DisInfoItem, v: String?): String {
        return if (v != null){
            "${item.name}:$v$newLine"
        } else ""
    }

    fun getDeviceInfoValue(deviceAddress: String, item: DisInfoItem): String {
        return formatLine(item, deviceInfoMap.get(deviceAddress)?.get(item)?.let { it as String })
    }

    override fun onDisconnected(deviceAddress: String) {
        deviceInfoMap.remove(deviceAddress)
    }

    override fun onConnected(address: String) {
        // nothing to do yet
    }

    fun setDeviceInfoValue(peripheral: BluetoothPeripheral, item: DisInfoItem, itemValue: String){
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(item, itemValue)
    }

}