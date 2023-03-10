package com.philips.bleclient.service.rcs

interface RCSServiceHandlerListener {
    abstract fun onConnected(address: String)
    abstract fun onDisconnected(address: String)
}