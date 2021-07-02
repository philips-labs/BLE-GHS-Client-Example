/*
 * Copyright (c) Koninklijke Philips N.V., 2021.
 * All rights reserved.
 */
package com.philips.btclient.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.btclient.acom.AcomObject
import com.philips.btclient.acom.Observation
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK

import io.mockk.*
import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
class GenericHealthSensorServiceHandlerTest {

    private val fakeAddress = "12:34:56:65:43:21"
    private val fakeDeviceName = "GHS Oral Thermometer"
    private lateinit var handler: GenericHealthSensorServiceHandler

    @MockK
    private lateinit var listener: GenericHealthSensorHandlerListener

    @MockK
    private lateinit var peripheral: BluetoothPeripheral

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { peripheral.address } returns fakeAddress
        every { peripheral.name } returns fakeDeviceName

        handler = GenericHealthSensorServiceHandler()
        handler.addListener(listener)
    }

    @Test
    fun `Given a byte array of two observations, when data is parsed, then an AcomObject with two observations is created`() {
        val byteArray = byteArrayOf(
            // First Observation
            0x00,
            0x01,
            0x09,
            0x2F,
            0x00,
            0x04,
            0x00,
            0x02,
            0xE0.toByte(),
            0x08, // Type: Obs Type, Length: 4, value: Oral Temp
            0x00,
            0x01,
            0x09,
            0x21,
            0x00,
            0x02,
            0x00,
            0x01,                      // Type: Handle, Length: 2, Value: 0x0001
            0x00,
            0x01,
            0x0A,
            0x56,
            0x00,
            0x04,
            0xFF.toByte(),
            0x00,
            0x01,
            0x7A, // Type: Simple Num, Length: 4, value: 38.7
            0x00,
            0x01,
            0x09,
            0x96.toByte(),
            0x00,
            0x04,
            0x00.toByte(),
            0x04,
            0x17,
            0xA0.toByte(), // Type: Unit Code, Length: 4, value: Celsius

            0x00,
            0x01,
            0x09,
            0x90.toByte(),
            0x00,
            0x08,
            0x00,
            0x00,
            0x01,
            0x76,
            0x24,
            0x1E,
            0xE8.toByte(),
            0x82.toByte(), // Type: Abs Timestamp, Length: 8, value: Wed Dec 02 2020 15:42:54
//            0x00, 0x01, 0x09, 0x90.toByte(), 0x00, 0x08, 0x20, 0x20, 0x11, 0x26, 0x16, 0x43, 0x44, 0x77, // Type: Abs Timestamp, Length: 8, value: 2020/11/26 16:43:44.77

            // Second Observation
            0x00,
            0x01,
            0x09,
            0x2F,
            0x00,
            0x04,
            0x00,
            0x02,
            0xE0.toByte(),
            0x08, // Type: Obs Type, Length: 4, value: Oral Temp
            0x00,
            0x01,
            0x09,
            0x21,
            0x00,
            0x02,
            0x00,
            0x01,                      // Type: Handle, Length: 2, Value: 0x0001
            0x00,
            0x01,
            0x0A,
            0x56,
            0x00,
            0x04,
            0xFF.toByte(),
            0x00,
            0x01,
            0x7A, // Type: Simple Num, Length: 4, value: 38.7
            0x00,
            0x01,
            0x09,
            0x96.toByte(),
            0x00,
            0x04,
            0x00.toByte(),
            0x04,
            0x17,
            0xA0.toByte(), // Type: Unit Code, Length: 4, value: Celsius

            0x00,
            0x01,
            0x09,
            0x90.toByte(),
            0x00,
            0x08,
            0x00,
            0x00,
            0x01,
            0x76,
            0x24,
            0x1E,
            0xE8.toByte(),
            0x82.toByte() // Type: Abs Timestamp, Length: 8, value: Wed Dec 02 2020 15:42:54
//            0x00, 0x01, 0x09, 0x90.toByte(), 0x00, 0x08, 0x20, 0x20, 0x11, 0x26, 0x16, 0x43, 0x44, 0x77 // Type: Abs Timestamp, Length: 8, value: 2020/11/26 16:43:44.77


        )
        val acomObject = AcomObject(byteArray)
        assertNotNull(acomObject)
        assertTrue { acomObject.observations.size == 2 }
        assertTrue { acomObject.observations.first().type.value == ObservationType.MDC_TEMP_ORAL.value }
        assertTrue { acomObject.observations.last().type.value == ObservationType.MDC_TEMP_ORAL.value }
    }

    @Test
    fun `Given a GHS service handler with a listener, when an observation characteristic occurs, then the observations are sent to listener`() {
        val characteristic =
            spyk(
                BluetoothGattCharacteristic(
                    GenericHealthSensorServiceHandler.OBSERVATION_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )
            )

        every { characteristic.uuid } returns GenericHealthSensorServiceHandler.OBSERVATION_CHARACTERISTIC_UUID

        val segment1 = byteArrayOf(
            // Segment header, first segment, segemnt #1
            0b101,
            // Type: Obs Type, Length: 4, value: Oral Temp
            0x00, 0x01, 0x09, 0x2F, 0x00, 0x04, 0x00, 0x02, 0xE0.toByte(), 0x08
        )

        val segment2 = byteArrayOf(
            // Segment header, middle segment, segemnt #2
            0b1000,
            // Type: Handle, Length: 2, Value: 0x0001
            0x00, 0x01, 0x09, 0x21, 0x00, 0x02, 0x00, 0x01
        )

        val segment3 = byteArrayOf(
            // Segment header, middle segment, segemnt #3
            0b1100,
            // Type: Simple Num, Length: 4, value: 38.7
            0x00, 0x01, 0x0A, 0x56, 0x00, 0x04, 0xFF.toByte(), 0x00, 0x01, 0x7A
        )

        val segment4 = byteArrayOf(
            // Segment header, middle segment, segemnt #4
            0b10000,
            // Type: Unit Code, Length: 4, value: Celsius
            0x00, 0x01, 0x09, 0x96.toByte(), 0x00, 0x04, 0x00.toByte(), 0x04, 0x17, 0xA0.toByte(),
        )

        val segment5 = byteArrayOf(
            // Segment header, last segment, segemnt #5
            0b10110,
            // Type: Abs Timestamp, Length: 8, value: Wed Dec 02 2020 15:42:54
            0x00,
            0x01,
            0x09,
            0x90.toByte(),
            0x00,
            0x08,
            0x00,
            0x00,
            0x01,
            0x76,
            0x24,
            0x1E,
            0xE8.toByte(),
            0x82.toByte()
        )

        val segments = listOf(segment1, segment2, segment3, segment4, segment5)

        val observations = slot<List<Observation>>()

        // Given
        every { listener.onReceivedObservations(any(), capture(observations)) } answers { }

        // When
        segments.forEach {
            handler.onCharacteristicUpdate(
                peripheral,
                it,
                characteristic,
                GattStatus.SUCCESS
            )
        }

        // Then
        val obsList = observations.captured
        assertNotNull(obsList)
    }

    @Test
    fun `Given a GHS service handler with a listener, when an 2 observation characteristic occurs, then the observations are sent to listener`() {
        val characteristic =
            spyk(
                BluetoothGattCharacteristic(
                    GenericHealthSensorServiceHandler.OBSERVATION_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )
            )

        every { characteristic.uuid } returns GenericHealthSensorServiceHandler.OBSERVATION_CHARACTERISTIC_UUID

        val segment1 = byteArrayOf(
            // Segment header, first segment, segemnt #1
            0b101,
            // Type: Obs Type, Length: 4, value: Oral Temp
            0x00, 0x01, 0x09, 0x2F, 0x00, 0x04, 0x00, 0x02, 0xE0.toByte(), 0x08
        )

        val segment2 = byteArrayOf(
            // Segment header, middle segment, segemnt #2
            0b1000,
            // Type: Handle, Length: 2, Value: 0x0001
            0x00, 0x01, 0x09, 0x21, 0x00, 0x02, 0x00, 0x01
        )

        val segment3 = byteArrayOf(
            // Segment header, middle segment, segemnt #3
            0b1100,
            // Type: Simple Num, Length: 4, value: 38.7
            0x00, 0x01, 0x0A, 0x56, 0x00, 0x04, 0xFF.toByte(), 0x00, 0x01, 0x7A
        )

        val segment4 = byteArrayOf(
            // Segment header, middle segment, segemnt #4
            0b10000,
            // Type: Unit Code, Length: 4, value: Celsius
            0x00, 0x01, 0x09, 0x96.toByte(), 0x00, 0x04, 0x00.toByte(), 0x04, 0x17, 0xA0.toByte(),
        )

        val segment5 = byteArrayOf(
            // Segment header, middle segment, segemnt #5
            0b10100,
            // Type: Abs Timestamp, Length: 8, value: Wed Dec 02 2020 15:42:54
            0x00,
            0x01,
            0x09,
            0x90.toByte(),
            0x00,
            0x08,
            0x00,
            0x00,
            0x01,
            0x76,
            0x24,
            0x1E,
            0xE8.toByte(),
            0x82.toByte()
        )

        val segment6 = byteArrayOf(
            // Segment header, middle segment, segemnt #6
            0b11000,
            // Type: Obs Type, Length: 4, value: Heart Rate
            0x00, 0x01, 0x09, 0x2F, 0x00, 0x04, 0x00, 0x02, 0x41, 0x82.toByte()
        )

        val segment7 = byteArrayOf(
            // Segment header, middle segment, segemnt #7
            0b11100,
            // Type: Handle, Length: 2, Value: 0x0002
            0x00, 0x01, 0x09, 0x21, 0x00, 0x02, 0x00, 0x02
        )

        val segment8 = byteArrayOf(
            // Segment header, middle segment, segemnt #8
            0b100000,
            // Type: Simple Num, Length: 4, value: 66
            0x00, 0x01, 0x0A, 0x56, 0x00, 0x04, 0x00, 0x00, 0x00, 0x42
        )

        val segment9 = byteArrayOf(
            // Segment header, middle segment, segemnt #9
            0b100100,
            // Type: Unit Code, Length: 4, value: bpm 0x00040AA0
            0x00, 0x01, 0x09, 0x96.toByte(), 0x00, 0x04, 0x00.toByte(), 0x04, 0x0A, 0xA0.toByte(),
        )

        val segment10 = byteArrayOf(
            // Segment header, last segment, segemnt #10
            0b101010,
            // Type: Abs Timestamp, Length: 8, value: Wed Dec 02 2020 15:42:54
            0x00,
            0x01,
            0x09,
            0x90.toByte(),
            0x00,
            0x08,
            0x00,
            0x00,
            0x01,
            0x76,
            0x24,
            0x1E,
            0xE8.toByte(),
            0x82.toByte()
        )

        val segments = listOf(
            segment1,
            segment2,
            segment3,
            segment4,
            segment5,
            segment6,
            segment7,
            segment8,
            segment9,
            segment10
        )

        val observations = slot<List<Observation>>()

        // Given
        every { listener.onReceivedObservations(any(), capture(observations)) } answers { }

        // When
        segments.forEach {
            handler.onCharacteristicUpdate(
                peripheral,
                it,
                characteristic,
                GattStatus.SUCCESS
            )
        }

        // Then
        val obsList = observations.captured
        assertNotNull(obsList)
    }

}