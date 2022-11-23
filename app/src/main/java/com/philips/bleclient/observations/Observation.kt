/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.extensions.*
import com.philips.bleclient.util.ObservationClass
import com.philips.bleclient.util.ObservationHeaderFlags
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import com.philips.mjolnir.services.handlers.generichealthsensor.acom.MdcConstants
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime
import timber.log.Timber
import java.nio.ByteOrder

@Suppress("unused")
open class Observation {
    var handle: Int? = null
    var type: ObservationType = ObservationType.UNKNOWN
    var timestamp: LocalDateTime? = null
    var timeCounter: Long? = null
    open var value: ObservationValue? = null
    var unitCode: UnitCode = UnitCode.UNKNOWN_CODE

    /*
     * The following are defined in ACOM but currently not used/set in the current example...
     * For example, developer may want to have a current patent id in the app and can set
     * observations created to have the pateint id of the user
     */
    var measurementDuration: Int? = null
    var patientId: Int? = null
    var specializationCodes: Array<Int>? = null

    constructor(
        id: Int?,
        type: ObservationType,
        value: Float,
        valuePrecision: Int,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) {
        this.handle = id
        this.type = type
        val obsVal = SimpleNumericObservationValue(value, unitCode)
        obsVal.accuracy = valuePrecision
        this.value = obsVal
        this.unitCode = unitCode
        this.timestamp = timestamp
        this.patientId = patientId
    }

    constructor(
        id: Int,
        type: ObservationType,
        value: String,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, StringObservationValue(value), unitCode, timestamp, patientId)

    constructor(
        id: Int,
        type: ObservationType,
        value: Int,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, DiscreteObservationValue(value), unitCode, timestamp, patientId)


    constructor(
        id: Int,
        type: ObservationType,
        value: ByteArray,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, SampleArrayObservationValue(value, unitCode), unitCode, timestamp, patientId)

    constructor(
        id: Int,
        type: ObservationType,
        observations: List<Observation>,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, BundledObservationValue(observations), UnitCode.UNKNOWN_CODE, timestamp, patientId)

    constructor(
        id: Int,
        type: ObservationType,
        compoundValue: CompoundNumericValue,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, compoundValue, UnitCode.UNKNOWN_CODE, timestamp, patientId)

    constructor(
        id: Int,
        type: ObservationType,
        sampleArrayValue: SampleArrayObservationValue,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, sampleArrayValue, UnitCode.UNKNOWN_CODE, timestamp, patientId)

    constructor(
        id: Int,
        type: ObservationType,
        value: ObservationValue,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) {
        this.handle = id
        this.type = type
        this.value = value
        this.unitCode = unitCode
        this.timestamp = timestamp
        this.patientId = patientId
    }

    constructor(bytesParser: BluetoothBytesParser) {
        var firstTime = true
        while (!bytesParser.atEnd() && (firstTime || !bytesParser.isNextAttributeType())) {
            firstTime = false
            getNextAttribute(bytesParser)
        }
    }

    override fun toString(): String {
        return "Observation: ${type.name} $value time: $timestamp"
    }

    private fun getNextAttribute(bytesParser: BluetoothBytesParser) {
        val attributeType = getAttributeType(bytesParser) ?: return
        val length = getAttributeLength(bytesParser) ?: return
        when (attributeType) {
            MdcConstants.MDC_ATTR_ID_TYPE -> getObservationTypeAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_ID_HANDLE -> getHandleAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_NU_VAL_OBS_SIMP -> getSimpleNumericValueAttribute(bytesParser)
            MdcConstants.MDC_ATTR_NU_VAL_OBS -> getSimpleNumericValueAttribute(bytesParser)
            MdcConstants.MDC_ATTR_NU_CMPD_VAL_OBS -> getCompoundNumericValueAttribute(
                bytesParser,
                length
            )
            MdcConstants.MDC_ATTR_SA_VAL_OBS -> getSampleArrayValueAttribute(bytesParser, length)
            MdcConstants.MDC_ATTR_TIME_STAMP_ABS -> getAbsoluteTimestampAttribute(
                bytesParser,
                length
            )
            MdcConstants.MDC_ATTR_UNIT_CODE -> getUnitCodeAttribute(bytesParser, length)
            // MDC_ATTR_PERSON_ID is not defined in the latest 10101R document, but specified as the ACOM Observation attribute type
//            MdcConstants.MDC_ATTR_PERSON_ID -> getPateintIdAttribute(bytesParser, length)
            // MDC_ATTR_TIME_PD_MSMT_ACTIVE_ACOM is not defined in the latest 10101R document, but specified as the Observation
            // measurement duration attribute type
//            MdcConstants.MDC_ATTR_TIME_PD_MSMT_ACTIVE_ACOM -> getMeasurementDurationAttribute(bytesParser, length)
            // TODO: 11/26/20  MDC_ATTR_SUPPLEMENTAL_INFO is not defined in the latest 10101R document, however MDC_ATTR_SUPPLEMENTAL_TYPES is
            // Using that as a placeholder until defined or perhaps info recast to types
            MdcConstants.MDC_ATTR_SUPPLEMENTAL_TYPES -> getSupplementalInformationAttribute(
                bytesParser,
                length
            )
            else -> return
        }
    }

    private fun getObservationTypeAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        if (length != ATTRIBUTE_OBSERVATION_TYPE_LENGTH) return
        type =
            ObservationType.fromValue(bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
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
        value = SimpleNumericObservationValue(numValue, unitCode)
    }

    private fun getSampleArrayValueAttribute(bytesParser: BluetoothBytesParser, length: Int) {
        value = getSampleArrayValue(bytesParser)
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
        if (timeFlags hasFlag TimestampFlags.isTickCounter) {
            timeCounter = bytesParser.getGHSTimeCounter()
        } else {
            timestamp = bytesParser.getGHSDateTime(timeFlags)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getSupplementalInformationAttribute(
        bytesParser: BluetoothBytesParser,
        length: Int
    ) {
        TODO("not implemented")
    }

    companion object {
        const val ATTRIBUTE_HANDLE_LENGTH = 0x02
        const val ATTRIBUTE_OBSERVATION_TYPE_LENGTH = 0x04
        const val ATTRIBUTE_UNIT_CODE_LENGTH = 0x04
        const val ATTRIBUTE_ABSOLUTE_TIMESTAMP_LENGTH = 0x08

        const val FLAGS_COMPOUND_NUMERIC_OBSERVATION_VALUE = 0x4
        const val FLAGS_BUNDLED_OBSERVATION_VALUE = 0xF

        const val MDC_SYSTEM_URN_STRING = "urn:iso:std:iso:11073:10101"
        const val CODE_SYSTEM_OBSERVATRION_CATEGORY_URL =
            "http://terminology.hl7.org/CodeSystems/observation_category"

        // Continue parsing the observation after it is determined the bytes in parser are a BundledObservation
        private fun bundledObservationFrom(
            flags: BitMask,
            parser: BluetoothBytesParser
        ): Observation {
            val attributesMap = mutableMapOf<String, Any>()
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp =
                getTimestampIfPresent(flags, parser)?.let { attributesMap.put("timestamp", it); it }
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId =
                getObjectIdIfPresent(flags, parser)?.let { attributesMap.put("objectId", it); it }
            val patientId = getPatientIdIfPresent(flags, parser)
            patientId?.let { attributesMap.put("patientId", it) }
            getSupplementalInfoIfPresent(flags, parser)?.let {
                attributesMap.put(
                    "supplementalInfo",
                    it
                )
            }
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

            val bundledObservations = getBundledObservations(parser)
            disaggragateBundledObservationValues(bundledObservations, attributesMap)
            return Observation(objectId ?: 0, observationType, bundledObservations, timestamp, patientId)
        }

        private fun getBundledObservations(parser: BluetoothBytesParser): List<Observation> {
            val observations = mutableListOf<Observation>()
            val numberOfObs = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            repeat(numberOfObs) {
                getObservationFrom(parser)?.let { observation -> observations.add(observation) }
            }
            return observations
        }

        private fun disaggragateBundledObservationValues(
            observations: List<Observation>,
            commonValues: Map<String, Any>
        ) {
            observations.forEach { observation ->
                commonValues.get("patientId")?.let { observation.patientId = it as Int }
                commonValues.get("timestamp")?.let { observation.timestamp = it as LocalDateTime }
                observation.timestamp
            }
        }

        private fun compoundNumericObservationFrom(
            flags: BitMask,
            parser: BluetoothBytesParser
        ): Observation? {
            // TODO DRY This with bundle observations
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp = getTimestampIfPresent(flags, parser)
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId = getObjectIdIfPresent(flags, parser)
            val patientId = getPatientIdIfPresent(flags, parser)
            val supplementalInfo = getSupplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

            val values = getCompoundNumericValues(parser)
            return Observation(objectId ?: 0, observationType, values, timestamp, patientId)
        }

        private fun getCompoundNumericValues(parser: BluetoothBytesParser): CompoundNumericValue {
            val values = mutableListOf<SimpleNumericObservationValue>()
            val numValues = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            repeat(numValues) {
                val type = getObservationType(parser)
                val unitCode = UnitCode.readFrom(parser)
                val value = parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
                // TODO Add type to SimpleNumericObservationValue
                values.add(SimpleNumericObservationValue(value, unitCode))
            }
            return CompoundNumericValue(values)
        }

        // This could be merged back into the main obs creation and just do the value in the switch on obs class
        private fun sampleArrayObservationFrom(
            flags: BitMask,
            parser: BluetoothBytesParser
        ): Observation {
            // TODO DRY This with bundle observations
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp = getTimestampIfPresent(flags, parser)
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId = getObjectIdIfPresent(flags, parser)
            val patientId = getPatientIdIfPresent(flags, parser)
            val supplementalInfo = getSupplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

            val values = getSampleArrayValue(parser)
            return Observation(objectId ?: 0, observationType, values, timestamp, patientId)
        }

        // This could be merged back into the main obs creation and just do the value in the switch on obs class
        private fun stringObservationFrom(
            flags: BitMask,
            parser: BluetoothBytesParser
        ): Observation {
            // TODO DRY This with bundle observations
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp = getTimestampIfPresent(flags, parser)
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId = getObjectIdIfPresent(flags, parser)
            val patientId = getPatientIdIfPresent(flags, parser)
            val supplementalInfo = getSupplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

            val stringVal = getStringValue(parser)
            return Observation(objectId ?: 0, observationType, stringVal,  unitCode = UnitCode.UNKNOWN_CODE, timestamp, patientId)
        }

        private fun getStringValue(bytesParser: BluetoothBytesParser): String {
            val length = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            val bytes = bytesParser.getByteArray(length)
            return String(bytes)
        }

        // This could be merged back into the main obs creation and just do the value in the switch on obs class
        private fun discreteObservationFrom(
            flags: BitMask,
            parser: BluetoothBytesParser
        ): Observation {
            // TODO DRY This with bundle observations
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp = getTimestampIfPresent(flags, parser)
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId = getObjectIdIfPresent(flags, parser)
            val patientId = getPatientIdIfPresent(flags, parser)
            val supplementalInfo = getSupplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

            val discreteVal = getDiscreteValue(parser)
            return Observation(objectId ?: 0, observationType, discreteVal,  unitCode = UnitCode.UNKNOWN_CODE, timestamp, patientId)
        }

        private fun getDiscreteValue(bytesParser: BluetoothBytesParser): Int {
            return bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
        }

        private fun getSampleArrayValue(bytesParser: BluetoothBytesParser): SampleArrayObservationValue {
            val unitCode = UnitCode.readFrom(bytesParser)
            val scaleFactor = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val offset = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val scaledMin = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)
            val scaledMax = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)
            val samplePeriod = bytesParser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            val samplesPerPeriod = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            val bytesPerSample = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
            val numberOfSamples = bytesParser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)

            val byteArray = bytesParser.getByteArray(bytesPerSample * numberOfSamples)
            return SampleArrayObservationValue(
                byteArray,
                scaleFactor,
                offset,
                scaledMin,
                scaledMax,
                samplePeriod,
                samplesPerPeriod,
                bytesPerSample,
                numberOfSamples,
                unitCode
            )
        }

        /*
         * The Observation Type field contains a 4-byte MDC code from IEEE 11073-10101 [6] that uniquely defines the type of the observation.
         * This field is mandatory for single observations that are not part of a bundle, and it is optional for observation bundles.
         * For single observations in bundles, either the bundle contains the Observation Type field that applies to all contained single
         * observations or each single observation contains Observation Type field.
         */
        private fun getObservationTypeIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): ObservationType {
            return if (observationFlags.hasFlag(ObservationHeaderFlags.isObservationTypePresent)) {
                getObservationType(parser)
            } else {
                ObservationType.UNKNOWN
            }
        }

        /*
         * The optional Measurement Duration field contains the duration of the measurement in seconds, reported as a floating number.
         */
        private fun getDurationIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): Float? {
            return if (observationFlags.hasFlag(ObservationHeaderFlags.isMeasurementDurationPresent)) {
                parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            } else {
                null
            }
        }

        private fun getTimestampIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): LocalDateTime? {
            var timestamp: LocalDateTime? = null
            var timecounter: Long? = null
            if (observationFlags.hasFlag(ObservationHeaderFlags.isTimestampPresent)) {
                val timestampFlags =
                    BitMask(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toLong())
                if (timestampFlags.hasFlag(TimestampFlags.isTickCounter)) {
                    // TODO What sort of "time" represents the tick counter... or we need to handle returning a counter
                } else {
                    timecounter = parser.getGHSTimeCounter()
                    val syncSource = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                    val timeOffset = parser.getIntValue(BluetoothBytesParser.FORMAT_SINT8)
                    timestamp = timecounter.asKotlinLocalDateTime(timestampFlags, timeOffset)
                }
            }
            return timestamp
        }

        /*
         * The Measurement Status field is defined as a series of Boolean conditions.
         */
        private fun getMeasurmentStatusIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): BitMask? {
            return if (observationFlags.hasFlag(ObservationHeaderFlags.isMeasurementStatusPresent)) {
                return BitMask(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16).toLong())
            } else {
                null
            }
        }

        /*
         * The Object Id field contains a value that can be used in other observations to reference this specific observation.
         * The value shall be unique in the context of its usage, which is a bundle of Observations.
         */
        private fun getObjectIdIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): Int? {
            return if (observationFlags.hasFlag(ObservationHeaderFlags.isObjectIdPresent)) {
                parser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)
            } else {
                null
            }
        }

        /*
         * This optional field contains a local identification of the patient or user using the sensor device.
         */
        private fun getPatientIdIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): Int? {
            return if (observationFlags.hasFlag(ObservationHeaderFlags.isPatientIdPresent)) {
                parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
            } else {
                null
            }
        }

        /*
         * This optional field contains supplemental information that can help in understanding the observation.
         * It can be used to report the body location of the sensor, the meal context of an observation,
         * or other relevant aspects. Only information that can be expressed by an MDC code can be reported.
         */
        private fun getSupplementalInfoIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ): List<Int>? {
            var codes: MutableList<Int>? = null
            // TODO Grab Supplemental Information (var # bytes)
            if (observationFlags.hasFlag(ObservationHeaderFlags.isSupplementalInformationPresent)) {
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                codes = mutableListOf()
                repeat(count) { codes.add(parser.getIntValue(BluetoothBytesParser.FORMAT_SINT32)) }
            }
            return codes?.toList()
        }

        /*
         * The Derived From field contains references to other Observations from which this Observation is derived.
         * To references an Observations its Object Id value is used.
         */
        private fun getDerivedFromIfPresent(
            observationFlags: BitMask,
            parser: BluetoothBytesParser
        ) {
            if (observationFlags.hasFlag(ObservationHeaderFlags.isDerivedFromPresent)) {
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                repeat(count) {
                    val objId = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
                    Timber.i("Derived from object id: $objId")
                }
            }
        }

        /*
         * The Has Member field is present in Observations that are defined by a series of other Observations.
         * An example would be an exercise session on a fitness machine that reports the type of the
         * session and references the individual heart rate measurements taken during the session. The Has Member
         * field contains the references to the other Observations that are a member of this group of Observations.
         */
        private fun getHasMemberIfPresent(observationFlags: BitMask, parser: BluetoothBytesParser) {
            if (observationFlags.hasFlag(ObservationHeaderFlags.hasMember)) {
                throw RuntimeException("Observation with Has Member Flag Unsupported")
            }
        }

        /*
         * ACOM allows addition of new object types to the model as well as the addition of new attributes
         * to defined object types. To support new attributes in Observations TLVs are used. TLV stands
         * for Type-Length-Value. For the Type an MDC code is used that typically comes from partition 1,
         * the Object-oriented partition and have a Reference Id starting with “MDC_ATTR_”.
         */
        private fun getTLVsIfPresent(observationFlags: BitMask, parser: BluetoothBytesParser) {
            if (observationFlags.hasFlag(ObservationHeaderFlags.hasTLVPresent)) {
                throw RuntimeException("Observation with Has TLVs Flag Unsupported")
            }
        }

        private fun getObservationType(parser: BluetoothBytesParser): ObservationType {
            return ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32))
        }

        // We need to pass in the length of bytes in the parser since BluetoothBytesParser has no method
        // or property to access the size of the private mValue byte array
        private fun getObservationFrom(
            parser: BluetoothBytesParser,
            bytesLength: Int = 0
        ): Observation? {
            val observationClass = ObservationClass.fromValue(
                parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte()
            )
            val length = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)

            // validate the length (if passed in) and return null if invalid
             if (length != bytesLength) return null

            val observationFlags =
                BitMask(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16).toLong())

            return when (observationClass) {
                ObservationClass.ObservationBundle -> {
                    bundledObservationFrom(observationFlags, parser)
                }
                ObservationClass.CompoundNumeric -> {
                    compoundNumericObservationFrom(observationFlags, parser)
                }
                ObservationClass.RealTimeSampleArray -> {
                    sampleArrayObservationFrom(observationFlags, parser)
                }
                ObservationClass.String -> {
                    stringObservationFrom(observationFlags, parser)
                }
                ObservationClass.SimpleDiscreet -> {
                    discreteObservationFrom(observationFlags, parser)
                }
                else -> {
                    val observationType = getObservationTypeIfPresent(observationFlags, parser)
                    val timestamp = getTimestampIfPresent(observationFlags, parser)

                    // Do we want to use the kotlin.time.Duration class?
                    val measurementDuration = getDurationIfPresent(observationFlags, parser)
                    val measurementStatus = getMeasurmentStatusIfPresent(observationFlags, parser)
                    val objId = getObjectIdIfPresent(observationFlags, parser)
                    val patientId = getPatientIdIfPresent(observationFlags, parser)
                    val supplimentalInfoCodes = getSupplementalInfoIfPresent(observationFlags, parser)

                    // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
                    // so for now just check if present and throw and exception if set
                    getDerivedFromIfPresent(observationFlags, parser)
                    getHasMemberIfPresent(observationFlags, parser)
                    getTLVsIfPresent(observationFlags, parser)

                    // Unit code, float is true only for a simple numeric
                    val unitCode = UnitCode.readFrom(parser)

                    val observationValue: Float = when (observationClass) {
                        ObservationClass.SimpleNumeric -> parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
                        else -> 0f
                    }

                    Observation(
                        id = objId,
                        type = observationType,
                        value = observationValue,
                        valuePrecision = 2,
                        unitCode = unitCode,
                        timestamp = timestamp,
                        patientId = patientId
                    )
                }
            }
        }

        fun fromBytes(ghsFixedFormatBytes: ByteArray): Observation? {
            return getObservationFrom(
                BluetoothBytesParser(ghsFixedFormatBytes, 0, ByteOrder.LITTLE_ENDIAN),
                ghsFixedFormatBytes.size
            )
        }

    }

}
