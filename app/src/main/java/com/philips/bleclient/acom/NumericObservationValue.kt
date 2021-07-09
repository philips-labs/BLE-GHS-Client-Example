/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.acom

@Suppress("unused")
open class NumericObservationValue: ObservationValue() {
    var accuracy: Int? = null

    companion object {
        const val SIMPLE_NUMERIC_VALUE_LENGTH = 0x04
    }

}