/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.util.ObservationClass
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import com.welie.blessed.BluetoothBytesParser
import timber.log.Timber

abstract class ObservationValue {
    open var unitCode: UnitCode = UnitCode.UNKNOWN_CODE

    companion object {
        fun from(clazz: ObservationClass, bytesParser: BluetoothBytesParser): ObservationValue {
            return when (clazz) {
                ObservationClass.TLVEncoded -> getTLVObservationValue(bytesParser)
                ObservationClass.CompoundDiscreteEvent -> getCompoundDiscreteValue(bytesParser)
                ObservationClass.String -> getStringValue(bytesParser)
                ObservationClass.CompoundState -> getCompoundStateValue(bytesParser)
                ObservationClass.RealTimeSampleArray -> getSampleArrayValue(bytesParser)
                ObservationClass.CompoundObservation -> getCompoundObservationValue(bytesParser)
                ObservationClass.SimpleDiscreet -> getDiscreteValue(bytesParser)
                ObservationClass.SimpleNumeric -> getSimpleNumericObservationValue(bytesParser)
                else -> UnknownObservationValue()
            }
        }

        private fun getSimpleNumericObservationValue(bytesParser: BluetoothBytesParser): SimpleNumericObservationValue {
            // Unit code, float is true only for a simple numeric
            val unitCode = UnitCode.readFrom(bytesParser)
            val value = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            return SimpleNumericObservationValue(value, unitCode)
        }

        private fun getDiscreteValue(bytesParser: BluetoothBytesParser): DiscreteObservationValue {
            return DiscreteObservationValue(bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
        }

        private fun getSampleArrayValue(bytesParser: BluetoothBytesParser): SampleArrayObservationValue {
            val unitCode = UnitCode.readFrom(bytesParser)
            val scaleFactor = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val offset = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val samplePeriod = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val samplesPerPeriod = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            val bytesPerSample = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            val numberOfSamples = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
            val byteArray = bytesParser.getByteArray(bytesPerSample * numberOfSamples)

            return SampleArrayObservationValue(
                byteArray,
                scaleFactor,
                offset,
                samplePeriod,
                samplesPerPeriod,
                bytesPerSample,
                numberOfSamples,
                unitCode
            )
        }

        private fun getCompoundStateValue(bytesParser: BluetoothBytesParser): CompoundStateObservationValue {
            val size = bytesParser.getUInt8()
            val supportedMaskBits = bytesParser.getByteArray(size)
            val stateOrEventMaskBits = bytesParser.getByteArray(size)
            val bits = bytesParser.getByteArray(size)
            return CompoundStateObservationValue(supportedMaskBits, stateOrEventMaskBits, bits)
        }

        private fun getTLVObservationValue(bytesParser: BluetoothBytesParser): TLVObservationValue {
            val numValues = bytesParser.getUInt8()
            val list = mutableListOf<Pair<Int, Long>>()
            repeat(numValues) {
                val type = bytesParser.getUInt32()
                val length = bytesParser.getUInt16()
                val formatType = bytesParser.getUInt8()
                val bytes = bytesParser.getByteArray(length)
                val value = getBluetoothValue(formatType, bytes)
                list.add(Pair(type, value))
            }
            return TLVObservationValue(list)
        }

        private fun getCompoundObservationValue(parser: BluetoothBytesParser): CompoundObservationValue {
            val values = mutableListOf<ObservationComponent>()
            val numValues = parser.getUInt8()
            repeat(numValues) {
                val componentType = ObservationType.fromValue(parser.getUInt32()) // MDC Code for the component value
                val componentValueType = ObservationComponentValueType.fromValue(parser.getUInt8().toUByte()) // Data type for component value
                val componentValue = getComponentValue(componentValueType, parser) // Value itself (based on data type)
                val observationComponent = ObservationComponent(componentType, componentValue)
                values.add(observationComponent)
            }
            return CompoundObservationValue(values)
        }

        private fun getComponentValue(componentValueType: ObservationComponentValueType, parser: BluetoothBytesParser): ObservationValue {
            return when(componentValueType) {
                ObservationComponentValueType.SimpleNumeric -> getSimpleNumericObservationValue(parser)
                ObservationComponentValueType.String -> getStringValue(parser)
                ObservationComponentValueType.SimpleDiscreet -> getDiscreteValue(parser)
                ObservationComponentValueType.CompoundDiscreteEvent -> getCompoundDiscreteValue(parser)
                ObservationComponentValueType.RealTimeSampleArray -> getSampleArrayValue(parser)
                ObservationComponentValueType.CompoundState -> getCompoundStateValue(parser)
                ObservationComponentValueType.Unknown -> UnknownObservationValue()
            }
        }

        private fun getStringValue(bytesParser: BluetoothBytesParser): StringObservationValue {
            val length = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            val bytes = bytesParser.getByteArray(length)
            val str = String(bytes)
            Timber.i("getStringValue of len: $length from bytes: ${bytes.asFormattedHexString()} value: $str")
            return StringObservationValue(str)
        }

        private fun getCompoundDiscreteValue(bytesParser: BluetoothBytesParser): CompoundDiscreetObservationValue {
            val numValues = bytesParser.getUInt8()
            val list = mutableListOf<Int>()
            repeat(numValues) { list.add(bytesParser.getUInt32()) }
            return CompoundDiscreetObservationValue(list)
        }

        private fun getBluetoothValueLength(formatType: Int): Int {
            return when (formatType) {
                4 -> 1
                6 -> 2
                8 -> 4
                9 -> 6
                10 -> 8
                else -> 0
            }
        }

        private fun getBluetoothValue(formatType: Int, bytes: ByteArray): Long {
            val parser = BluetoothBytesParser(bytes)
            return when (formatType) {
                4 -> parser.getUInt8().toLong()
                6 -> parser.getUInt16().toLong()
                8 -> parser.getUInt32().toLong()
                9 -> parser.getUInt48()
                10 -> parser.getUInt64()
                else -> -1.toLong()
            }
        }

    }
}