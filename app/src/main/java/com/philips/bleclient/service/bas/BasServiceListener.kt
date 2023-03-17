package com.philips.bleclient.service.bas

interface BasServiceListener  {

        fun onBatteryLevel(deviceAddress: String, batteryLevel: Int)

        abstract fun onConnected(address: String)
        abstract fun onDisconnected(address: String)
}