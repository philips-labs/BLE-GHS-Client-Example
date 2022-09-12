package com.philips.bleclient.observations

class DiscreteObservationValue(val value: Int): ObservationValue() {
    override fun toString(): String {
        return "Discrete value: $value"
    }
}