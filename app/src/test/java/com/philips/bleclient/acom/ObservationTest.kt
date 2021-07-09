/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.acom

import com.philips.btserver.generichealthservice.ObservationType
import com.philips.btserver.generichealthservice.UnitCode
import org.junit.Assert.*
import org.junit.Test
import kotlinx.datetime.toKotlinLocalDateTime

class ObservationTest {

    private val observationType = ObservationType.MDC_ECG_HEART_RATE
    private val randomValue = ObservationType.MDC_ECG_HEART_RATE.randomNumericValue()
    private val randomSampleArray = ObservationType.MDC_PPG_TIME_PD_PP.randomSampleArray()
    private val now = java.time.LocalDateTime.now().toKotlinLocalDateTime()

    @Test
    fun `When a SimpleNumericObservation is instantiated, then all properties are initialized correctly`() {
        val obs = create_simple_numeric_observation()
        assertEquals(100, obs.handle)
        assertEquals(observationType, obs.type)
        val obsVal = obs.value
        val isSimple = obsVal is SimpleNumericObservationValue
        assertTrue(isSimple)

        assertEquals(randomValue, (obs.value as SimpleNumericObservationValue).value)
        assertEquals(UnitCode.MDC_DIM_BEAT_PER_MIN, obs.unitCode)
        assertEquals(
            UnitCode.MDC_DIM_BEAT_PER_MIN,
            (obs.value as SimpleNumericObservationValue).unitCode
        )
        assertEquals(now, obs.timestamp)
    }

    @Test
    fun `When a SampleArrayObservation is instantiated, then all properties are initialized correctly`() {
        val obs = create_sample_array_observation()
        assertEquals(100, obs.handle)
        assertEquals(observationType, obs.type)
        assertArrayEquals(randomSampleArray, (obs.value as SampleArrayObservationValue).samples)
        assertEquals(UnitCode.MDC_DIM_BEAT_PER_MIN, obs.unitCode)
        assertEquals(now, obs.timestamp)
    }

    /*
     * Private test support and utility methods
     */

    private fun create_simple_numeric_observation(): Observation {

        return Observation(
            id = 100,
            type = observationType,
            value = randomValue,
            valuePrecision = observationType.numericPrecision(),
            unitCode = UnitCode.MDC_DIM_BEAT_PER_MIN,
            timestamp = now
        )
    }

    private fun create_sample_array_observation(): Observation {
        return Observation(
            id = 100,
            type = observationType,
            value = randomSampleArray,
            unitCode = UnitCode.MDC_DIM_BEAT_PER_MIN,
            timestamp = now
        )
    }
}

fun ObservationType.randomNumericValue(): Float {
    return when (this) {
        ObservationType.MDC_ECG_HEART_RATE -> kotlin.random.Random.nextInt(60, 70).toFloat()
        ObservationType.MDC_TEMP_BODY -> kotlin.random.Random.nextInt(358, 370).toFloat() / 10f
        ObservationType.MDC_SPO2_OXYGENATION_RATIO -> kotlin.random.Random.nextInt(970, 990)
            .toFloat() / 10f
        else -> Float.NaN
    }
}

// For now regardless of type the sample array is just totally random and alway a 255 element byte array (thus observation type is unused)
fun ObservationType.randomSampleArray(): ByteArray {
    val numberOfCycles = 5
    val samplesPerSecond = kotlin.random.Random.nextInt(40, 70)
    val sampleSeconds = 5
    val buffer = ByteArray(samplesPerSecond * sampleSeconds)
    buffer.fillWith { i ->
        (Math.sin(numberOfCycles * (2 * Math.PI) * i / samplesPerSecond) * 200).toInt().toByte()
    }
    return buffer
}

fun ObservationType.numericPrecision(): Int {
    return when (this) {
        ObservationType.MDC_ECG_HEART_RATE -> 0
        ObservationType.MDC_TEMP_BODY,
        ObservationType.MDC_SPO2_OXYGENATION_RATIO -> 1
        else -> 0
    }
}

fun ObservationType.unitCode(): UnitCode {
    return when (this) {
        ObservationType.MDC_ECG_HEART_RATE -> UnitCode.MDC_DIM_BEAT_PER_MIN
        ObservationType.MDC_TEMP_BODY -> UnitCode.MDC_DIM_DEGC
        ObservationType.MDC_SPO2_OXYGENATION_RATIO -> UnitCode.MDC_DIM_PERCENT
        ObservationType.MDC_PPG_TIME_PD_PP -> UnitCode.MDC_DIM_INTL_UNIT
        else -> UnitCode.MDC_DIM_INTL_UNIT
    }
}

fun ObservationType.shortUnitCode(): UnitCode {
    return when (this) {
        ObservationType.MDC_ECG_HEART_RATE -> UnitCode.MDC_DIM_BEAT_PER_MIN
        ObservationType.MDC_TEMP_BODY -> UnitCode.MDC_DIM_DEGC
        ObservationType.MDC_SPO2_OXYGENATION_RATIO -> UnitCode.MDC_DIM_PERCENT
        ObservationType.MDC_PPG_TIME_PD_PP -> UnitCode.MDC_DIM_INTL_UNIT
        else -> unitCode()
    }
}

fun ByteArray.fillWith(action: (Int) -> Byte) {
    for (i in 0 until size) {
        this[i] = action(i)
    }
}
