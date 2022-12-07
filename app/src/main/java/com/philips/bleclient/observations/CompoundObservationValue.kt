package com.philips.bleclient.observations

import com.philips.btserver.generichealthservice.ObservationType

class CompoundObservationValue(val values: List<ObservationComponent>): ObservationValue() {
//    override fun toString(): String {
//        return "Compound Value: ${values.forEach { it.toString() + "\n" } }"
//    }
}