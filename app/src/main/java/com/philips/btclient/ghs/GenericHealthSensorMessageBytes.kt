/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.ghs

data class GenericHealthSensorMessageBytes(
    var receivedMessageBytes : ByteArray = ByteArray(0),
    var lastSequenceNumber : Byte  = 0xFF.toByte()
) {

}