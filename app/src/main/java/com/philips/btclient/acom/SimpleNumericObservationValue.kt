/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.acom

class SimpleNumericObservationValue(val value: Float): NumericObservationValue() {

    override fun toString(): String {
        return "SimpleNumericObservationValue value: $value"
    }
}