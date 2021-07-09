/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.acom

import com.philips.btserver.generichealthservice.UnitCode

class SimpleNumericObservationValue(val value: Float, override var unitCode: UnitCode): NumericObservationValue() {
    override fun toString(): String {
        return "value: $value unitCode: ${unitCode.name}"
    }
}