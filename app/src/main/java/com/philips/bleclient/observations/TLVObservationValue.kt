package com.philips.bleclient.observations

import com.philips.bleclient.asFormattedHexString

class TLVObservationValue(val values: List<Pair<Int, Long>>): ObservationValue() {
    override fun toString(): String {
        var resultString = "{\n"
        values.forEach {
            resultString += "  [${it.first}, ${it.second}] \n"
        }
        resultString += " }"
        return "TLVObservationValue: $resultString"
    }
}