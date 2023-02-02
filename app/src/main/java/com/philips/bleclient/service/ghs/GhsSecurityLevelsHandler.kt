package com.philips.bleclient.service.ghs

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.ui.ObservationLog
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.nio.ByteOrder

class GhsSecurityLevelsHandler(val service: GenericHealthSensorServiceHandler) {
    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
            Timber.i( "LE GATT Security Level Bytes: ${value.asFormattedHexString()}")
            ObservationLog.log("LE GATT Security Level Bytes: ${value.asFormattedHexString()}")
    }

}