/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.acom

open class NumericObservationValue: ObservationValue() {
    var unitCode: Int? = null
    var accuracy: Int? = null

    companion object {
        const val SIMPLE_NUMERIC_VALUE_LENGTH = 0x04
    }

}