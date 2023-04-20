/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.philips.bleclient.extensions.*
import com.philips.bleclient.toHexString
import com.philips.bleclient.util.ObservationClass
import com.philips.bleclient.util.ObservationFlagBitMask
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
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
    var measurementDuration: Float? = null
    var measurementStatus: BitMask? = null
    var patientId: Int? = null
    var specializationCodes: Array<Int>? = null
    var supplementalInformation: List<Int>? = null
    var isCurrentTimeline = true

    constructor(
        id: Int,
        type: ObservationType,
        floatValue: Float,
        valuePrecision: Int,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(id, type, SimpleNumericObservationValue(floatValue, unitCode, valuePrecision), unitCode, timestamp, patientId)

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
        patientId: Int? = null
    ) :
            this(
                id,
                type,
                SampleArrayObservationValue(value, unitCode),
                unitCode,
                timestamp,
                patientId
            )

    constructor(
        id: Int,
        type: ObservationType,
        observations: List<Observation>,
        timestamp: LocalDateTime?,
        patientId: Int?
    ) :
            this(
                id,
                type,
                BundledObservationValue(observations),
                UnitCode.UNKNOWN_CODE,
                timestamp,
                patientId
            )

    constructor(
        id: Int,
        type: ObservationType,
        compoundValue: CompoundObservationValue,
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
        value.also { this.value = it }
        this.unitCode = unitCode
        this.timestamp = timestamp
        this.patientId = patientId
    }

    override fun toString(): String {
        return "Observation: ${type.name} patient: $patientId val: $value unit: $unitCode time: $timestamp isCurrentTimeLine: $isCurrentTimeline"
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
            flags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Observation {
            val attributesMap = mutableMapOf<String, Any>()
            val observationType = observationTypeIfPresent(flags, parser)
            val timestamp =
                timestampIfPresent(flags, parser).let {
                    it.first?.let { attributesMap.put("timestamp", it) }
                    attributesMap.put("isCurrentTimeline", it.second)
                    it.first }
            val measurementDuration = getDurationIfPresent(flags, parser)?.let { attributesMap.put("measurementDuration", it); it }
            val measurementStatus = measurmentStatusIfPresent(flags, parser)?.let { attributesMap.put("measurementStatus", it); it }
            val objectId = objectIdIfPresent(flags, parser)?.let { attributesMap.put("objectId", it); it }
            val patientId = patientIdIfPresent(flags, parser)
            patientId?.let { attributesMap.put("patientId", it) }
            val supplementalInformation = supplementalInfoIfPresent(flags, parser)?.let { attributesMap.put("supplementalInfo", it); it }

            derivedFromIfPresent(flags, parser)
            hasMemberIfPresent(flags, parser)
            val tlvs = getTLVsIfPresent(flags, parser)

            val bundledObservations = getBundledObservations(parser)
            disaggragateBundledObservationValues(bundledObservations, attributesMap)
            val obs =  Observation(
                objectId ?: 0,
                observationType,
                bundledObservations,
                timestamp,
                patientId
            )
            obs.measurementDuration = measurementDuration
            obs.measurementStatus = measurementStatus
            obs.supplementalInformation = supplementalInformation
            return obs
        }

        private fun getBundledObservations(parser: BluetoothBytesParser): List<Observation> {
            val observations = mutableListOf<Observation>()
            val numberOfObs = parser.getUInt8()
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

        private fun buildObservationFrom(
            observationClass: ObservationClass,
            flags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Observation {
            // TODO DRY This with bundle observations
            val observationType = observationTypeIfPresent(flags, parser)

            val timestampPair = timestampIfPresent(flags, parser)
            val timestamp = timestampPair.first
            val isCurrentTimeline = timestampPair.second
            val measurementDuration = getDurationIfPresent(flags, parser)
            val measurementStatus = measurmentStatusIfPresent(flags, parser)
            val objectId = objectIdIfPresent(flags, parser)
            val patientId = patientIdIfPresent(flags, parser)
            val supplementalInfo = supplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            val derivedFrom = derivedFromIfPresent(flags, parser)
            val hasMembers = hasMemberIfPresent(flags, parser)
            val tlvs = getTLVsIfPresent(flags, parser)

            val observationValue = ObservationValue.from(observationClass, parser)
            val obs = Observation(
                objectId ?: 0,
                observationType,
                observationValue,
                unitCode = UnitCode.UNKNOWN_CODE,
                timestamp,
                patientId
            )
            obs.isCurrentTimeline = isCurrentTimeline
            return obs
        }

        /*
         * The Observation Type field contains a 4-byte MDC code from IEEE 11073-10101 [6] that uniquely defines the type of the observation.
         * This field is mandatory for single observations that are not part of a bundle, and it is optional for observation bundles.
         * For single observations in bundles, either the bundle contains the Observation Type field that applies to all contained single
         * observations or each single observation contains Observation Type field.
         */
        private fun observationTypeIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): ObservationType = if (observationFlags.isObservationTypePresent) getObservationType(parser) else ObservationType.UNKNOWN

        /*
         * The optional Measurement Duration field contains the duration of the measurement in seconds, reported as a floating number.
         */
        private fun getDurationIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Float? = if (observationFlags.isMeasurementDurationPresent) parser.float else null

        private fun timestampIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Pair<LocalDateTime?, Boolean> {
            var timestamp: LocalDateTime? = null
            val timecounter: Long?
            var isCurrentTimeline = true
            if (observationFlags.isTimestampPresent) {
                val timestampFlags = BitMask(parser.uInt8.toLong())
                Timber.i("Read Observation Timestamp Flags: ${timestampFlags.value.toHexString()}")
                if (timestampFlags.hasFlag(TimestampFlags.isTickCounter)) {
                    // TODO What sort of "time" represents the tick counter... or we need to handle returning a counter
                } else {
                    timecounter = parser.getGHSTimeCounter()
                    // Sync source is read, but is not a component of the timestamp so unused
                    val syncSource = parser.uInt8
                    val timeOffset = parser.sInt8
                    timestamp = timecounter.asKotlinLocalDateTime(timestampFlags, timeOffset)
                }

                isCurrentTimeline = timestampFlags hasFlag TimestampFlags.isCurrentTimeline
            }
            return Pair(timestamp, isCurrentTimeline)
        }

        /*
         * The Measurement Status field is defined as a series of Boolean conditions.
         */
        private fun measurmentStatusIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): BitMask? = if (observationFlags.isMeasurementStatusPresent) BitMask(parser.uInt16) else null

        /*
         * The Object Id field contains a value that can be used in other observations to reference this specific observation.
         * The value shall be unique in the context of its usage, which is a bundle of Observations.
         */
        private fun objectIdIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Int? = if (observationFlags.isObjectIdPresent) parser.uInt32 else null


        /*
         * This optional field contains a local identification of the patient or user using the sensor device.
         */
        private fun patientIdIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Int? = if (observationFlags.isPatientIdPresent) parser.getUInt8() else null

        /*
         * This optional field contains supplemental information that can help in understanding the observation.
         * It can be used to report the body location of the sensor, the meal context of an observation,
         * or other relevant aspects. Only information that can be expressed by an MDC code can be reported.
         */
        private fun supplementalInfoIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): List<Int>? {
            var codes: MutableList<Int>? = null
            if (observationFlags.isSupplementalInformationPresent) {
                codes = mutableListOf()
                repeat(parser.uInt8) { codes.add(parser.sInt32) }
            }
            return codes?.toList()
        }

        /*
         * The Derived From field contains references to other Observations from which this Observation is derived.
         * To references an Observations its Object Id value is used.
         */
        private fun derivedFromIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): MutableList<Int>? {
            var derivedFrom: MutableList<Int>? = null
            if (observationFlags.isDerivedFromPresent) {
                derivedFrom = mutableListOf()
                repeat(parser.uInt8) { derivedFrom.add(parser.uInt32) }
            }
            return derivedFrom
        }

        /*
         * The Has Member field is present in Observations that are defined by a series of other Observations.
         * An example would be an exercise session on a fitness machine that reports the type of the
         * session and references the individual heart rate measurements taken during the session. The Has Member
         * field contains the references to the other Observations that are a member of this group of Observations.
         */
        private fun hasMemberIfPresent(observationFlags: ObservationFlagBitMask, parser: BluetoothBytesParser) : MutableList<Int>? {
            var hasMember: MutableList<Int>? = null
            if (observationFlags.hasMember) {
                hasMember = mutableListOf()
                repeat(parser.uInt8) { hasMember.add(parser.uInt32) }
            }
            return hasMember
        }

        /*
         * ACOM allows addition of new object types to the model as well as the addition of new attributes
         * to defined object types. To support new attributes in Observations TLVs are used. TLV stands
         * for Type-Length-Value. For the Type an MDC code is used that typically comes from partition 1,
         * the Object-oriented partition and have a Reference Id starting with “MDC_ATTR_”.
         */
        private fun getTLVsIfPresent(observationFlags: ObservationFlagBitMask, parser: BluetoothBytesParser): TLVObservationValue? {
            return if (observationFlags.hasTLVPresent) parser.tlvObservationValue() else null
        }

        private fun getObservationType(parser: BluetoothBytesParser): ObservationType = ObservationType.fromValue(parser.uInt32)

        // We need to pass in the length of bytes in the parser since BluetoothBytesParser has no method
        // or property to access the size of the private mValue byte array
        private fun getObservationFrom(
            parser: BluetoothBytesParser,
            bytesLength: Int = 0
        ): Observation? {
            val observationClass = ObservationClass.fromValue(parser.uInt8.toUByte())
            val length = parser.uInt16

            // validate the length (if passed in) and return null if invalid
            // if (length != bytesLength) return null

            val observationFlags = ObservationFlagBitMask.from(parser)

            return when (observationClass) {
                ObservationClass.CompoundObservation,
                ObservationClass.RealTimeSampleArray,
                ObservationClass.String,
                ObservationClass.SimpleDiscreet,
                ObservationClass.CompoundDiscreteEvent,
                ObservationClass.CompoundState,
                ObservationClass.TLVEncoded,
                ObservationClass.SimpleNumeric -> {
                    buildObservationFrom(observationClass, observationFlags, parser)
                }
                ObservationClass.ObservationBundle -> {
                    bundledObservationFrom(observationFlags, parser)
                }
                else -> null
            }
        }

        fun fromBytes(ghsFixedFormatBytes: ByteArray): Observation? {
            Timber.i("Construction observation from ${ghsFixedFormatBytes.size} bytes.")
            return getObservationFrom(
                BluetoothBytesParser(ghsFixedFormatBytes, 0, ByteOrder.LITTLE_ENDIAN),
                ghsFixedFormatBytes.size
            )
        }

    }
}
