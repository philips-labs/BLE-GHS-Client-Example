/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import kotlinx.datetime.LocalDateTime

class SimpleNumericObservationValue(val value: Float, override var unitCode: UnitCode): NumericObservationValue() {

    constructor(
        value: Float,
        unitCode: UnitCode,
        accuracy: Int?
    ) : this(value, unitCode) {
        this.accuracy = accuracy
    }

    override fun toString(): String {
        return "value: $value ${unitCode.description}"
    }
}