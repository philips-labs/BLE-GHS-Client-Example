package com.philips.bleclient.service.dis

import com.philips.bleclient.service.bas.BasServiceListener
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

object DisInfoMap : DisServiceListener, BasServiceListener {

    val deviceInfoMap = mutableMapOf<String, MutableMap<DisInfoItem, Any>>()

    fun getInfo(address: String): String {
        val info : String = DisInfoItem.values().fold("Device information:\n", { string, item -> string + getDeviceInfoValue(address, item)})
        Timber.i(info)
        return info
    }

    fun formatLine(item: DisInfoItem, v: String): String {
        return if (v.isEmpty()) "" else "${item.name}: $v\n"
    }

    fun getDeviceInfoValue(deviceAddress: String, item: DisInfoItem): String {
        val div : String = formatLine(item, deviceInfoMap.get(deviceAddress)?.get(item)?.let { it as String } ?: "")
        Timber.i("InfoMap: ${item.name } - $div")
        return div
    }

    override fun onDisconnected(address: String) {
        deviceInfoMap.remove(address)
    }

    override fun onBatteryLevel(deviceAddress: String, batteryLevel: Int) {
        val batteryInfoItem : DisInfoItem = DisInfoItem.BATTERY_LEVEL
        deviceInfoMap.getOrPut(deviceAddress) { mutableMapOf()}.put(batteryInfoItem, "$batteryLevel%")
    }

    override fun onConnected(address: String) {
        // nothing to do yet
    }

    fun setDeviceInfoValue(peripheral: BluetoothPeripheral, item: DisInfoItem, itemValue: String){
        deviceInfoMap.getOrPut(peripheral.address) { mutableMapOf() }.put(item, itemValue)
    }

}