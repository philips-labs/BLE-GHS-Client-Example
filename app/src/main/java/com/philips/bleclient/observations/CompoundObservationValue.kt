package com.philips.bleclient.observations

class CompoundObservationValue(val values: List<ObservationComponent>): ObservationValue() {
    override fun toString(): String {
        return "Compound Value with ${values.size} values: [\n ${values.fold("", {s, o -> s + o.toString() + "\n" }) } ]"
    }
}