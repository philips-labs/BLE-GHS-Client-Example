/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.acom

import com.philips.btclient.asHexString

class SampleArrayObservationValue(val samples: ByteArray): ObservationValue() {
    override fun toString(): String {
        return "SampleArrayObservationValue length: ${samples.size}  bytes: ${samples.asHexString()}"
    }

}