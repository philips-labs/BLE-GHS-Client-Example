package com.philips.bleclient.observations

class CompoundNumericValue(val values: List<SimpleNumericObservationValue>): ObservationValue() {
//    override fun toString(): String {
//        return "Compound Value: ${values.forEach { it.toString() + "\n" } }"
//    }
}