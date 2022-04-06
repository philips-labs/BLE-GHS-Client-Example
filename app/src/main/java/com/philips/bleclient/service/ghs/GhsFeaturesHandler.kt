package com.philips.bleclient.service.ghs

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.nio.ByteOrder

class GhsFeaturesHandler(val service: GenericHealthSensorServiceHandler) {

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        // Handle case where features hasn't been set up on the server (shouldn't happen, but safety)
        if (value.size < 2) return
        Timber.i( "Features characteristic update bytes: <${value.asFormattedHexString()}> for peripheral: ${peripheral.address}")
        val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
        val featuresFlags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        val numberOfObsTypes = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        // Ensure the number of bytes matches what we expect (Flags, number of types and 4 bytes per type
        if (value.size >= (numberOfObsTypes * 4) + 2) {
            val supportedTypes = mutableListOf<ObservationType>()
            repeat (numberOfObsTypes) {
                supportedTypes.add(ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)))
            }
            Timber.i( "Features characteristic update received obs types: <$supportedTypes>")
            service.receivedSupportedTypes(peripheral.address, supportedTypes)
        } else {
            Timber.i( "Error in features characteristic bytes size: ${value.size} expected: ${(numberOfObsTypes * 4) + 1}")
        }
        // Only flag is for Supported Device Specializations field present
        if (featuresFlags > 0) {
            handleFeaturesDeviceSpecializations(parser)
        }
    }

    private fun handleFeaturesDeviceSpecializations(parser: BluetoothBytesParser) {
        val numberOfDeviceSpecializations = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        Timber.i( "Number of device specializations: $numberOfDeviceSpecializations")
        repeat(numberOfDeviceSpecializations, {
            val deviceSpecBytes = parser.getByteArray(3)
            Timber.i( "Device specialization #${it + 1}: 00 08 ${deviceSpecBytes[1].asHexString()} ${deviceSpecBytes[0].asHexString()} ver: ${deviceSpecBytes[2]}")
        })
    }

}