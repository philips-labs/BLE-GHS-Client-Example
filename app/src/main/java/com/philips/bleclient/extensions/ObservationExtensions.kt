/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.philips.bleclient.extensions.BitMask
import com.philips.bleclient.observations.Observation
import com.philips.bleclient.observations.Observation.Companion.CODE_SYSTEM_OBSERVATRION_CATEGORY_URL
import com.philips.bleclient.observations.Observation.Companion.MDC_SYSTEM_URN_STRING
import com.philips.bleclient.observations.ObservationValue
import com.philips.bleclient.observations.SampleArrayObservationValue
import com.philips.bleclient.observations.SimpleNumericObservationValue
import com.philips.bleclient.extensions.Flags
import com.philips.bleclient.extensions.hasFlag
import com.philips.bleclient.toUINT8
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.json.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun Observation.toPPGSampleArray(): ByteArray {
    return this.value?.let {
        val ppgData = this.value as SampleArrayObservationValue
        ppgData.samples
    } ?: byteArrayOf()
}

fun Observation.timestampAsDate(): Date {
    return timestamp?.toJavaDate() ?: Date(0)
}

fun LocalDateTime.toJavaDate(): Date {
    return Date.from(
        this.toJavaLocalDateTime()
            .atZone(ZoneId.systemDefault())
            .toInstant()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun Observation.asFhir(): String {
    val zonedDateTime =
        ZonedDateTime.ofInstant(timestampAsDate().toInstant(), ZoneId.systemDefault())
    val dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime)
    val fhir = buildJsonObject {
        put("resourceType", "Observation")
        put("status", "final")
        putJsonArray("category") {
            addJsonObject {
                putJsonArray("coding") {
                    addJsonObject {
                        put("system", CODE_SYSTEM_OBSERVATRION_CATEGORY_URL)
                        put("code", "vital-signs")
                        put("display", "Vital Signs")
                    }
                }
                put("text", type.name)
            }
        }
        putJsonObject("code") {
            putJsonArray("coding") {
                addJsonObject {
                    put("system", MDC_SYSTEM_URN_STRING)
                    put("code", "${unitCode.value}")
                    put("display", unitCode.description)
                }
            }
        }
        put("effectiveDateTime", dateTime)
        value?.addToJsonBuilder(this)
    }
    return fhir.toString()
}

fun ObservationValue.addToJsonBuilder(builder: JsonObjectBuilder) {
    if (this is SimpleNumericObservationValue) {
        this.addToJsonBuilder(builder)
    } else if (this is SampleArrayObservationValue) {
        this.addToJsonBuilder(builder)
    }
}

fun SimpleNumericObservationValue.addToJsonBuilder(builder: JsonObjectBuilder) {
    builder.putJsonObject("valueQuantity") {
        put("value", value)
        put("unit", unitCode.symbol)
        put("system", "urn:iso:std:iso:11073:10101")
        put("code", "${unitCode.value}")
    }
}

fun ByteArray.toFhirString(): String {
    var resultString = ""
    for ((index, value) in this.iterator().withIndex()) {
        resultString += value.toUINT8().toString()
        if (index < (this.size - 1)) resultString += " "
    }
    return resultString
}

fun SampleArrayObservationValue.addToJsonBuilder(builder: JsonObjectBuilder) {
    builder.putJsonObject("valueSampledData") {
        putJsonObject("origin") {
            put("value", 0)
            put("system", MDC_SYSTEM_URN_STRING)
            put("code", "${unitCode.value}")
            put("display", unitCode.name)
        }
        put("data", samples.toFhirString())
        put("dimensions", 1)
        put("period", 1f / samples.size.toFloat())
        put("factor", 1)
        put("unit", unitCode.symbol)
    }
}

enum class ObservationHeaderFlags(override val bit: Long) : Flags {
    isObservationTypePresent(1 shl 0),
    isTimestampPresent(1 shl 1),
    isMeasurementDurationPresent(1 shl 2),
    isMeasurementStatusPresent(1 shl 3),
    isObjectIdPresent(1 shl 4),
    isPatientIdPresent(1 shl 5),
    isSupplementalInformationPresent(1 shl 6),
    isDerivedFromPresent(1 shl 7),
    hasMember(1 shl 8),
    hasTLVPresent(1 shl 9);
}


class ObservationFlagBitMask(value: Long) : BitMask(value) {
    val isObservationTypePresent get() = hasFlag(ObservationHeaderFlags.isObservationTypePresent)
    val isTimestampPresent get() = hasFlag(ObservationHeaderFlags.isTimestampPresent)
    val isMeasurementDurationPresent get() = hasFlag(ObservationHeaderFlags.isMeasurementDurationPresent)
    val isMeasurementStatusPresent get() = hasFlag(ObservationHeaderFlags.isMeasurementStatusPresent)
    val isObjectIdPresent get() = hasFlag(ObservationHeaderFlags.isObjectIdPresent)
    val isPatientIdPresent get() = hasFlag(ObservationHeaderFlags.isPatientIdPresent)
    val isSupplementalInformationPresent get() = hasFlag(ObservationHeaderFlags.isSupplementalInformationPresent)
    val isDerivedFromPresent get() = hasFlag(ObservationHeaderFlags.isDerivedFromPresent)
    val hasMember get() = hasFlag(ObservationHeaderFlags.hasMember)
    val hasTLVPresent get() = hasFlag(ObservationHeaderFlags.hasTLVPresent)

    companion object {
        fun from(parser: BluetoothBytesParser): ObservationFlagBitMask {
            return ObservationFlagBitMask(parser.getUInt16().toLong())
        }
    }
}

//enum class TimestampFlags(override val bit: Long) : Flags {
//    isTickCounter(1 shl 0),
//    isUTC(1 shl 1),
//    isMilliseconds(1 shl 2),
//    isHundredsMicroseconds(1 shl 3),
//    isTimezoneValid(1 shl 4),
//    isDSTValid(1 shl 5),
//    isCurrentTimeline(1 shl 6),
//}

enum class MeasurementStatusFlags(override val bit: Long) : Flags {
    invalid(1 shl 0),
    questionable(1 shl 1),
    notAvailable(1 shl 2),
    calibrating(1 shl 4),
    testData(1 shl 5),
    earlyEstimate(1 shl 6),
    thresholdError(1 shl 14),
    thresholdingDisabled(1 shl 15),
}

enum class ObservationClass(val value: UByte) {
    SimpleNumeric(0x00.toUByte()),
    SimpleDiscreet(0x01.toUByte()),
    String(0x02.toUByte()),
    RealTimeSampleArray(0x03.toUByte()),
    CompoundObservation(0x04.toUByte()),
    CompoundDiscreteEvent(0x05.toUByte()),
    CompoundState(0x06.toUByte()),
    TLVEncoded(0x07.toUByte()),
    ObservationBundle(0xFF.toUByte()),  // 0xFF
    Unknown(0xF0.toUByte());

    companion object {
        fun fromValue(value: UByte): ObservationClass {
            return values().find { it.value == value } ?: Unknown
        }
    }
}
