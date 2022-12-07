package com.philips.bleclient.observations

class CompoundStateObservationValue(
    supportedMaskBits: ByteArray,
    val stateOrEventMaskBits: ByteArray,
    val bits: ByteArray): ObservationValue() {
}