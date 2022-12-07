package com.philips.bleclient.observations

import com.philips.bleclient.asHexString

class CompoundDiscreetObservationValue(val values: List<Int>): ObservationValue() {
    override fun toString(): String {
        return "CompoundDiscreetObservationValue: $values"
    }

}