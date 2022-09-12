package com.philips.bleclient.observations

class StringObservationValue(val value: String): ObservationValue() {
    override fun toString(): String {
        return value
    }
}