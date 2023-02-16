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
        if (value.size > 1) {
            Timber.i( "Features characteristic update bytes: <${value.asFormattedHexString()}> for peripheral: ${peripheral.address}")
            val parser = BluetoothBytesParser(value, 0, ByteOrder.LITTLE_ENDIAN)
            val featuresFlags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)

            getSupportedTypes(parser)?.let {
                Timber.i( "Features characteristic update received obs types: <$it>")
                service.receivedSupportedTypes(peripheral.address, it)
                // Only flag is for Supported Device Specializations field present
                if (featuresFlags > 0) { service.receiveSupportedDeviceSpecializations(peripheral.address, getFeaturesDeviceSpecializations(parser)) }
            }
        } else {
            Timber.i( "Error in features characteristic bytes: ${value.asFormattedHexString()}")
        }
    }

    private fun getSupportedTypes(parser: BluetoothBytesParser): List<ObservationType>? {
        val numberOfObsTypes = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        return if (parser.bytesLength() < (numberOfObsTypes * 4) + 2) {
            Timber.i( "Error in features characteristic bytes size: ${parser.bytesLength()} expected: ${(numberOfObsTypes * 4) + 2}")
            null
        } else {
            val supportedTypes = mutableListOf<ObservationType>()
            repeat (numberOfObsTypes) {
                supportedTypes.add(ObservationType.fromValue(parser.getIntValue(BluetoothBytesParser.FORMAT_UINT32)))
            }
            supportedTypes.toList()
        }
    }

    private fun getFeaturesDeviceSpecializations(parser: BluetoothBytesParser) : List<DeviceSpecialization> {
        val numberOfDeviceSpecializations = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
        Timber.i( "Number of device specializations: $numberOfDeviceSpecializations")
        var specList = mutableListOf<DeviceSpecialization>()
        repeat(numberOfDeviceSpecializations) {
            val bl = parser.bytesLength()
            Timber.i("offset = ${parser.offset} it = $it bytesLength = $bl")
            if (parser.offset + 3 <= parser.bytesLength()){
                val deviceSpecTypeBytes = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16)
                val devSpec = DeviceSpecialization.fromValue(deviceSpecTypeBytes + 0x80000)
                val deviceSpecVersion = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8)
                specList.add(devSpec)
                Timber.i("Device specialization #${it + 1}: $devSpec version: $deviceSpecVersion")
            } else {
                Timber.i("Features characteristic > ATT_MTU - 3; performing a full read is needed.")
                return specList
            }
        }
        return specList
    }

}

private fun BluetoothBytesParser.bytesLength(): Int = value.size