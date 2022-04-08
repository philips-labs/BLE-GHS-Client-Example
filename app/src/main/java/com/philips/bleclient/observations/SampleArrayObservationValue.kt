/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.asHexString
import com.philips.btserver.generichealthservice.UnitCode

class SampleArrayObservationValue(val samples: ByteArray, override var unitCode: UnitCode): ObservationValue() {
    override fun toString(): String {
        return "SampleArrayObservationValue length: ${samples.size}  bytes: ${samples.asHexString()}"
    }
}