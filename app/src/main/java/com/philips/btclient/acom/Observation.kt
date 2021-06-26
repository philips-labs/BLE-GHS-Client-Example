/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.acom

import com.philips.btclient.extensions.atEnd
import com.philips.btclient.extensions.getAcomDateTime
import com.philips.btclient.extensions.getMderFloatValue
import com.philips.btclient.extensions.isNextAttributeType
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime

class Observation {
    var handle: Int? = null
    var type: Int? = null
    var timestamp: LocalDateTime? = null
    var measurementDuration: Int? = null
    var pateintId: Int? = null
    var value: ObservationValue? = null

    constructor(bytesParser: BluetoothBytesParser) {
        var firstTime = true
        while (!bytesParser.atEnd() && (firstTime || !bytesParser.isNextAttributeType())) {
            firstTime = false
            getNextAttribute(bytesParser)
        }
    }

    override fun toString(): String {
        return "Obs: ${ ObservationType.fromValue(type) } $timestamp\n$value"
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
            MdcConstants.MDC_ATTR_SUPPLEMENTAL_TYPES -> getSupplimentalInformationAttribute(bytesParser, length)
            else -> return
        }
    }

    private fun getObservationTypeAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_OBSERVATION_TYPE_LENGTH) return
        type = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
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
        value = SimpleNumericObservationValue( numValue )
    }

    private fun getSampleArrayValueAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        val byteArray = bytesParser.getByteArray(length)
        value = SampleArrayObservationValue( byteArray )
    }

    private fun getCompoundNumericValueAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        TODO("not implemented")
    }

    private fun getUnitCodeAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_UNIT_CODE_LENGTH) return
        val unitCode = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
        // TODO This assumes the observation value was before the unit code... what if it isn't (store in property?)
        if (value is NumericObservationValue) {
            (value as NumericObservationValue).unitCode = unitCode
        }
    }

    private fun getAbsoluteTimestampAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        // if (length != ATTRIBUTE_ABSOLUTE_TIMESTAMP_LENGTH) return
        timestamp = bytesParser.getAcomDateTime(length)
    }

    private fun getSupplimentalInformationAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        TODO("not implemented")
    }

    companion object {
        const val ATTRIBUTE_HANDLE_LENGTH = 0x02
        const val ATTRIBUTE_OBSERVATION_TYPE_LENGTH = 0x04
        const val ATTRIBUTE_UNIT_CODE_LENGTH = 0x04
        const val ATTRIBUTE_ABSOLUTE_TIMESTAMP_LENGTH = 0x08
    }

}
