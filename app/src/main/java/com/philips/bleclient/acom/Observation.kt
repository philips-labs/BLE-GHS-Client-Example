/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.acom

import com.philips.bleclient.extensions.atEnd
import com.philips.bleclient.extensions.getAcomDateTime
import com.philips.bleclient.extensions.getMderFloatValue
import com.philips.bleclient.extensions.isNextAttributeType
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime

class Observation {
    var handle: Int? = null
    var type: ObservationType = ObservationType.UNKNOWN_STATUS_CODE
    var timestamp: LocalDateTime? = null
    var measurementDuration: Int? = null
    var patientId: Int? = null
    var value: ObservationValue? = null
    var unitCode: UnitCode = UnitCode.UNKNOWN_CODE

    constructor(id: Int, type: ObservationType, value: Float, valuePrecision: Int, unitCode: UnitCode, timestamp: LocalDateTime){
        this.handle = id
        this.type = type
        val obsVal = SimpleNumericObservationValue(value, unitCode)
        obsVal.accuracy = valuePrecision
        this.value = obsVal
        this.unitCode = unitCode
        this.timestamp = timestamp
    }

    constructor(id: Int, type: ObservationType, value: ByteArray, unitCode: UnitCode, timestamp: LocalDateTime) :
        this(id, type, SampleArrayObservationValue(value, unitCode), unitCode, timestamp)

    constructor(id: Int, type: ObservationType, value: ObservationValue, unitCode: UnitCode, timestamp: LocalDateTime) {
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
        return "Obs: ${type.name} $timestamp\n$value"
    }

    private fun getNextAttribute(bytesParser: BluetoothBytesParser) {
        val attributeType =  getAttributeType(bytesParser) ?: return
        val length = getAttributeLength(bytesParser) ?: return
        when (attributeType) {
            MdcConstants.MDC_ATTR_ID_TYPE -> getObservationTypeAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_ID_HANDLE -> getHandleAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_NU_VAL_OBS_SIMP -> getSimpleNumericValueAttribute(bytesParser)
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
        timestamp = bytesParser.getAcomDateTime(length)
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
    }

}
