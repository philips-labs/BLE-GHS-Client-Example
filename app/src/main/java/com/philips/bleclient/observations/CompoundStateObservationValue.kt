package com.philips.bleclient.observations

import com.philips.bleclient.asFormattedHexString

class CompoundStateObservationValue(
    val supportedMaskBits: ByteArray,
    val stateOrEventMaskBits: ByteArray,
    val bits: ByteArray): ObservationValue() {
    override fun toString(): String {
        return "Compound State Value with supportedMaskBits: ${supportedMaskBits.asFormattedHexString()} \nstateOrEventMaskBits: ${stateOrEventMaskBits.asFormattedHexString()} \nbits: ${bits.asFormattedHexString()}"
    }
}