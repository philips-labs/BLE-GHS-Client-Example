/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.ghs

import com.philips.btclient.acom.AcomObject

interface GenericHealthSensorAcomBytesListener {

    @Suppress("unused")
    enum class ObservationError {
        InvalidObservationType,
        InvalidValueType,
        InvalidValue,
        InvalidDate
    }

    fun onReceivedAcomObject(deviceAddress: String, acomObject: AcomObject)

    fun onAcomError(deviceAddress: String, byteArray: ByteArray, error: ObservationError)

}