package com.philips.bleclient.observations

import com.philips.bleclient.util.ObservationClass
import com.philips.btserver.generichealthservice.ObservationType

enum class ObservationComponentValueType(val value: UByte) {
    SimpleNumeric(0x01.toUByte()),
    SimpleDiscreet(0x02.toUByte()),
    String(0x03.toUByte()),
    RealTimeSampleArray(0x04.toUByte()),
    CompoundDiscreteEvent(0x05.toUByte()),
    CompoundState(0x06.toUByte()),
    Unknown(0xF0.toUByte());

    companion object {
        fun fromValue(value: UByte): ObservationComponentValueType {
            return values().find { it.value == value } ?: Unknown
        }
    }
}

class ObservationComponent(val type: ObservationType, val value: ObservationValue) {
    override fun toString(): String {
        return "ObservationComponent type: $type type value: ${type.value} component value: $value"
    }
}