/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.asHexString
import com.philips.btserver.generichealthservice.UnitCode

class SampleArrayObservationValue(
    val samples: ByteArray,
    val scaleFactor: Float,
    val offset: Float,
    val samplePeriod: Float,
    val samplesPerPeriod: Int,
    val bytesPerSample: Int,
    val numberOfSamples: Int,
    override var unitCode: UnitCode
) : ObservationValue() {

    constructor(samples: ByteArray, unitCode: UnitCode) : this(
        samples,
        1.0f,
        0.0f,
        1.0f,
        1,
        1,
        samples.size,
        unitCode
    ) {

    }

    override fun toString(): String {
        return "SampleArrayObservationValue length: ${samples.size}  bytes: ${samples.asHexString()}"
    }
}