/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.ghs

import com.philips.btclient.acom.AcomObject

class GenericHealthSensorAcomBytesHandler(private val listener: GenericHealthSensorAcomBytesListener) {

    fun handleReceivedAcomBytes(deviceAddress: String, byteArray: ByteArray) {
        listener.onReceivedAcomObject(deviceAddress, AcomObject(byteArray))
    }

}
