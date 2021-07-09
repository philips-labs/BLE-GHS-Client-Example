/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.philips.bleclient.acom.Observation
import com.philips.bleclient.acom.Observation.Companion.CODE_SYSTEM_OBSERVATRION_CATEGORY_URL
import com.philips.bleclient.acom.Observation.Companion.MDC_SYSTEM_URN_STRING
import com.philips.bleclient.acom.ObservationValue
import com.philips.bleclient.acom.SampleArrayObservationValue
import com.philips.bleclient.acom.SimpleNumericObservationValue
import com.philips.bleclient.toUINT8
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
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
