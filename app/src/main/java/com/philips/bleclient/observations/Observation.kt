/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.bleclient.asHexString
import com.philips.bleclient.extensions.*
import com.philips.bleclient.util.ObservationClass
import com.philips.bleclient.util.ObservationFlagBitMask
import com.philips.bleclient.util.ObservationHeaderFlags
import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import com.welie.blessed.BluetoothBytesParser
import kotlinx.datetime.LocalDateTime
import okhttp3.internal.toHexString
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
        id: Int?,
        type: ObservationType,
        value: Float,
        valuePrecision: Int,
        unitCode: UnitCode,
        timestamp: LocalDateTime?,
        patientId: Int?,
        //supplementalInfo: List<Int>?
    ) {
        this.handle = id
        this.type = type
        val obsVal = SimpleNumericObservationValue(value, unitCode)
        obsVal.accuracy = valuePrecision
        this.value = obsVal
        this.unitCode = unitCode
        this.timestamp = timestamp
        this.patientId = patientId
        //this.supplementalInformation = supplementalInfo
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
        this.value = value
        this.unitCode = unitCode
        this.timestamp = timestamp
        this.patientId = patientId
    }

    override fun toString(): String {
        return "Observation: ${type.name} $value time: $timestamp isCurrentTimeLine: $isCurrentTimeline"
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
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestamp =
                getTimestampIfPresent(flags, parser).let {
                    it.first?.let { attributesMap.put("timestamp", it) }
                    attributesMap.put("isCurrentTimeline", it.second)
                    it.first }
            val measurementDuration = getDurationIfPresent(flags, parser)?.let { attributesMap.put("measurementDuration", it); it }
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)?.let { attributesMap.put("measurementStatus", it); it }
            val objectId =
                getObjectIdIfPresent(flags, parser)?.let { attributesMap.put("objectId", it); it }
            val patientId = getPatientIdIfPresent(flags, parser)
            patientId?.let { attributesMap.put("patientId", it) }
            val supplementalInformation = getSupplementalInfoIfPresent(flags, parser)?.let { attributesMap.put("supplementalInfo", it); it }

            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            getDerivedFromIfPresent(flags, parser)
            getHasMemberIfPresent(flags, parser)
            getTLVsIfPresent(flags, parser)

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
            val observationType = getObservationTypeIfPresent(flags, parser)
            val timestampPair = getTimestampIfPresent(flags, parser)
            val timestamp = timestampPair.first
            val isCurrentTimeline = timestampPair.second
            val measurementDuration = getDurationIfPresent(flags, parser)
            val measurementStatus = getMeasurmentStatusIfPresent(flags, parser)
            val objectId = getObjectIdIfPresent(flags, parser)
            val patientId = getPatientIdIfPresent(flags, parser)
            val supplementalInfo = getSupplementalInfoIfPresent(flags, parser)
            // Not dealing with Derived From, Has Member or TLVs present flags. If present this will go bad,
            // so for now just check if present and throw and exception if set
            val derivedFrom = getDerivedFromIfPresent(flags, parser)
            val hasMembers = getHasMemberIfPresent(flags, parser)
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
        private fun getObservationTypeIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): ObservationType {
            return if (observationFlags.isObservationTypePresent) {
                getObservationType(parser)
            } else {
                ObservationType.UNKNOWN
            }
        }

        /*
         * The optional Measurement Duration field contains the duration of the measurement in seconds, reported as a floating number.
         */
        private fun getDurationIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Float? {
            return if (observationFlags.isMeasurementDurationPresent) {
                parser.getFloatValue(BluetoothBytesParser.FORMAT_FLOAT)
            } else {
                null
            }
        }

        private fun getTimestampIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Pair<LocalDateTime?, Boolean> {
            var timestamp: LocalDateTime? = null
            var timecounter: Long? = null
            var isCurrentTimeline = true
            if (observationFlags.isTimestampPresent) {
                val timestampFlags =
                    BitMask(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toLong())
                Timber.i("Read Observation Timestamp Flags: ${timestampFlags.value.toHexString()}")
                if (timestampFlags.hasFlag(TimestampFlags.isTickCounter)) {
                    // TODO What sort of "time" represents the tick counter... or we need to handle returning a counter
                } else {
                    timecounter = parser.getGHSTimeCounter()
                    val syncSource = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                    val timeOffset = parser.getIntValue(BluetoothBytesParser.FORMAT_SINT8)
                    timestamp = timecounter.asKotlinLocalDateTime(timestampFlags, timeOffset)
                }

                isCurrentTimeline = timestampFlags hasFlag TimestampFlags.isCurrentTimeline
            }
            return Pair(timestamp, isCurrentTimeline)
        }

        /*
         * The Measurement Status field is defined as a series of Boolean conditions.
         */
        private fun getMeasurmentStatusIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): BitMask? {
            return if (observationFlags.isMeasurementStatusPresent) {
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
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Int? {
            return if (observationFlags.isObjectIdPresent) {
                parser.uInt32
            } else {
                null
            }
        }

        /*
         * This optional field contains a local identification of the patient or user using the sensor device.
         */
        private fun getPatientIdIfPresent(
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): Int? {
            return if (observationFlags.isPatientIdPresent) {
                parser.getUInt8()
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
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): List<Int>? {
            var codes: MutableList<Int>? = null
            // TODO Grab Supplemental Information (var # bytes)
            if (observationFlags.isSupplementalInformationPresent) {
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                Timber.i("Supplemental info count = $count")
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
            observationFlags: ObservationFlagBitMask,
            parser: BluetoothBytesParser
        ): MutableList<Int>? {
            var derivedFrom: MutableList<Int>? = null
            if (observationFlags.isDerivedFromPresent) {
                derivedFrom = mutableListOf()
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                Timber.i("Derived from count = $count")
                repeat(count) {
                    val objId = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
                    derivedFrom.add(objId)
                    Timber.i("Derived from object id: $objId")
                }
            }
            return derivedFrom
        }

        /*
         * The Has Member field is present in Observations that are defined by a series of other Observations.
         * An example would be an exercise session on a fitness machine that reports the type of the
         * session and references the individual heart rate measurements taken during the session. The Has Member
         * field contains the references to the other Observations that are a member of this group of Observations.
         */
        private fun getHasMemberIfPresent(observationFlags: ObservationFlagBitMask, parser: BluetoothBytesParser) : MutableList<Int>? {
            var hasMember: MutableList<Int>? = null
            if (observationFlags.hasMember) {
                hasMember = mutableListOf()
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                Timber.i("Has member count = $count")
                repeat(count) {
                    val objId = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
                    hasMember.add(objId)
                    Timber.i("Has member object id: $objId")
                }
            }
            return hasMember
        }

        /*
         * ACOM allows addition of new object types to the model as well as the addition of new attributes
         * to defined object types. To support new attributes in Observations TLVs are used. TLV stands
         * for Type-Length-Value. For the Type an MDC code is used that typically comes from partition 1,
         * the Object-oriented partition and have a Reference Id starting with “MDC_ATTR_”.
         */
        private fun getTLVsIfPresent(observationFlags: ObservationFlagBitMask, parser: BluetoothBytesParser) {
            if (observationFlags.hasTLVPresent) {
                val count = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                Timber.i("Number of TLVs = $count")
                repeat(count) {
                    val tlvType = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)
                    val tlvLength = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
                    val tlvFormatType = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8).toUByte().toInt()
                    val bytes = parser.getByteArray(tlvLength)
                    Timber.i("Type: $tlvType Length: $tlvLength Format: $tlvFormatType ${bytes.asHexString()}")
                }
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
