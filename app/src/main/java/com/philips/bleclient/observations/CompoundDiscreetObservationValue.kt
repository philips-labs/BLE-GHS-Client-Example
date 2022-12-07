package com.philips.bleclient.observations

class CompoundDiscreetObservationValue(val values: List<Int>): ObservationValue() {
    override fun toString(): String {
        return "CompoundDiscreetObservationValue: $values"
    }
}