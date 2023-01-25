package com.philips.bleclient.service.dis

interface DisServiceListener {
    abstract fun onConnected(address: String)
    abstract fun onDisconnected(address: String)
}