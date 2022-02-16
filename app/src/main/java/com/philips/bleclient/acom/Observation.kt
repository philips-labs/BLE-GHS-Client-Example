/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.acom

import com.philips.bleclient.extensions.*
import com.philips.bleclient.util.ObservationHeaderFlags
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime
import java.nio.ByteOrder
import java.util.*
import kotlin.time.Duration

@Suppress("unused")
class Observation {
    var handle: Int? = null
    var type: ObservationType = ObservationType.UNKNOWN_STATUS_CODE
    var timestamp: LocalDateTime? = null
    var timeCounter: Long? = null
    var value: ObservationValue? = null
    var unitCode: UnitCode = UnitCode.UNKNOWN_CODE

    /*
     * The following are defined in ACOM but currently not used/set in the current example...
     * For example, developer may want to have a current patent id in the app and can set
     * observations created to have the pateint id of the user
     */
    var measurementDuration: Int? = null
    var patientId: Int? = null
    var specializationCodes: Array<Int>? = null

    constructor(id: Int?, type: ObservationType, value: Float, valuePrecision: Int, unitCode: UnitCode, timestamp: LocalDateTime?){
        this.handle = id
        this.type = type
        val obsVal = SimpleNumericObservationValue(value, unitCode)
        obsVal.accuracy = valuePrecision
        this.value = obsVal
        this.unitCode = unitCode
        this.timestamp = timestamp
    }

    constructor(id: Int, type: ObservationType, value: ByteArray, unitCode: UnitCode, timestamp: LocalDateTime?) :
        this(id, type, SampleArrayObservationValue(value, unitCode), unitCode, timestamp)

    constructor(id: Int, type: ObservationType, value: ObservationValue, unitCode: UnitCode, timestamp: LocalDateTime?) {
        this.handle = id
        this.type = type
        this.value = value
        this.unitCode = unitCode
        this.timestamp = timestamp
    }

    constructor(bytesParser: BluetoothBytesParser) {
        var firstTime = true
        while (!bytesParser.atEnd() && (firstTime || !bytesParser.isNextAttributeType())) {
            firstTime = false
            getNextAttribute(bytesParser)
        }
    }

    override fun toString(): String {
        return "Obs ${type.name} value: $value $unitCode time: $timestamp"
    }

    private fun getNextAttribute(bytesParser: BluetoothBytesParser) {
        val attributeType =  getAttributeType(bytesParser) ?: return
        val length = getAttributeLength(bytesParser) ?: return
        when (attributeType) {
            MdcConstants.MDC_ATTR_ID_TYPE -> getObservationTypeAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_ID_HANDLE -> getHandleAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_NU_VAL_OBS_SIMP -> getSimpleNumericValueAttribute(bytesParser)
            MdcConstants.MDC_ATTR_NU_VAL_OBS -> getSimpleNumericValueAttribute(bytesParser)
            MdcConstants.MDC_ATTR_NU_CMPD_VAL_OBS -> getCompoundNumericValueAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_SA_VAL_OBS -> getSampleArrayValueAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_TIME_STAMP_ABS -> getAbsoluteTimestampAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_UNIT_CODE -> getUnitCodeAttribute(bytesParser, length)
            // MDC_ATTR_PERSON_ID is not defined in the latest 10101R document, but specified as the ACOM Observation attribute type
//            MdcConstants.MDC_ATTR_PERSON_ID -> getPateintIdAttribute(bytesParser, length)
            // MDC_ATTR_TIME_PD_MSMT_ACTIVE_ACOM is not defined in the latest 10101R document, but specified as the Observation
            // measurement duration attribute type
//            MdcConstants.MDC_ATTR_TIME_PD_MSMT_ACTIVE_ACOM -> getMeasurementDurationAttribute(bytesParser, length)
            // TODO: 11/26/20  MDC_ATTR_SUPPLEMENTAL_INFO is not defined in the latest 10101R document, however MDC_ATTR_SUPPLEMENTAL_TYPES is
            // Using that as a placeholder until defined or perhaps info recast to types
            MdcConstants.MDC_ATTR_SUPPLEMENTAL_TYPES -> getSupplementalInformationAttribute(bytesParser, length)
            else -> return
        }
    }

    private fun getObservationTypeAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_OBSERVATION_TYPE_LENGTH) return
        type = ObservationType.fromValue(bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
    }


    private fun getHandleAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_HANDLE_LENGTH) return
        handle = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
    }

    private fun getAttributeType(bytesParser: BluetoothBytesParser): Int? {
        return bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
    }

    private fun getAttributeLength(bytesParser: BluetoothBytesParser): Int? {
        return bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
    }

    private fun getSimpleNumericValueAttribute(bytesParser: BluetoothBytesParser) {
        val numValue = bytesParser.getMderFloatValue() ?: Float.NaN
        value = SimpleNumericObservationValue( numValue, unitCode)
    }

    private fun getSampleArrayValueAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        val byteArray = bytesParser.getByteArray(length)
        value = SampleArrayObservationValue( byteArray, unitCode)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getCompoundNumericValueAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        TODO("not implemented")
    }

    private fun getUnitCodeAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_UNIT_CODE_LENGTH) return
        // In case observation value isn't before the unit code... store it in the observation
        unitCode = UnitCode.fromValue(bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
        value?.unitCode = unitCode
    }

    private fun getAbsoluteTimestampAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        // if (length != ATTRIBUTE_ABSOLUTE_TIMESTAMP_LENGTH) return
        val timeFlags = bytesParser.getGHSDateTimeFlags()
        if (timeFlags hasFlag GhsTimestampFlags.isTickCounter) {
            timeCounter = bytesParser.getLongValue(ByteOrder.LITTLE_ENDIAN)
        } else {
            timestamp = bytesParser.getGHSDateTime(timeFlags)
        }
//        timestamp = bytesParser.getAcomDateTime(length)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getSupplementalInformationAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        TODO("not implemented")
    }

    companion object {
        const val ATTRIBUTE_HANDLE_LENGTH = 0x02
        const val ATTRIBUTE_OBSERVATION_TYPE_LENGTH = 0x04
        const val ATTRIBUTE_UNIT_CODE_LENGTH = 0x04
        const val ATTRIBUTE_ABSOLUTE_TIMESTAMP_LENGTH = 0x08

        const val MDC_SYSTEM_URN_STRING = "urn:iso:std:iso:11073:10101"
        const val CODE_SYSTEM_OBSERVATRION_CATEGORY_URL = "http://terminology.hl7.org/CodeSystems/observation_category"

        fun fromBytes(ghsFixedFormatBytes: ByteArray): Observation? {
            val parser = BluetoothBytesParser(ghsFixedFormatBytes, 0, ByteOrder.LITTLE_ENDIAN)
            val length = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            // validate the length and return null if invalid
            if (length != ghsFixedFormatBytes.size - 2) return null
            // TODO for now don't validate the CRC, but we'll need to when we're done with the first pass

            val observationFlags = BitMask(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32).toLong())
            val observationClass = observationFlags.value and 0xf
            val observationType: ObservationType = ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))

            var timestamp: LocalDateTime? = null
            var timecounter: Long? = null
            // Need to convert to seconds, milliseconds, 100ths microseconds
            if (observationFlags.hasFlag(ObservationHeaderFlags.isTimestampPresent)) {
                val timestampFlags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                timecounter = parser.getGHSTimeCounter()
                when(timestampFlags and 0xC0) {
                    // 00 - seconds
                    0 -> timecounter *= 1000
                    // 01 is milliseconds so no need to adjust
                    // 10 - 100usec
                    0x80 -> timecounter /= 1000
                }
                val syncSource = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                val timeOffset = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)

                // Check if ticks or clock
            }


            // Do we want to use the kotlin.time.Duration class?
            var measurementDuration: Float? = null
            if (observationFlags.hasFlag(ObservationHeaderFlags.isMeasurementDurationPresent)) {
                measurementDuration = parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            }

            var measurementStatus: Int? = null
            if (observationFlags.hasFlag(ObservationHeaderFlags.isMeasurementStatusPresent)) {
                measurementStatus = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            }

            var objId: Int? = null
            if (observationFlags.hasFlag(ObservationHeaderFlags.isObjectIdPresent)) {
                objId = parser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)
            }

            var patientId: Int? = null
            if (observationFlags.hasFlag(ObservationHeaderFlags.isPatientIdPresent)) {
                patientId = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            }

            var codes: MutableList<Int>? = null
            // TODO Grab Supplemental Information (var # bytes)
            if (observationFlags.hasFlag(ObservationHeaderFlags.isSupplementalInformationPresent)) {
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                codes = mutableListOf()
                repeat(count) { codes.add(parser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)) }
            }

            // TODO Grab Derived-from (var # bytes)
            // TODO Grab hasMember (var # bytes)

            if (observationFlags.hasFlag(ObservationHeaderFlags.isPatientIdPresent)) {
                patientId = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            }

            val unitCode: UnitCode = UnitCode.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
            val observationValue: Float = when (observationClass.toInt()) {
//                0 -> SimpleNumericObservationValue(0f, UnitCode.UNKNOWN_CODE)
                0 -> parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
                else -> 0f
            }

            return Observation(id = objId,
                    type = observationType,
                    value = observationValue,
                    valuePrecision = 2,
                    unitCode = unitCode,
                    timestamp = timestamp)

        }



    }

}
