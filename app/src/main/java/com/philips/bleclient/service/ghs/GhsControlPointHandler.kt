package com.philips.bleclient.service.ghs

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.asHexString
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler.Companion.GHS_CONTROL_POINT_CHARACTERISTIC_UUID
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.util.*

class GhsControlPointHandler(val service: GenericHealthSensorServiceHandler) {

    private val sentCommand: Byte? = null

    fun startLiveObservations() {
    }

    fun stopLiveObservations() {

    }

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Received Control Point Response Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        if (value.size > 0) {
            when(value[0]) {
            }
        }
    }

    companion object {
        /*
         * GHS Control Point commands and status values
         */
        private const val START_SEND_LIVE_OBSERVATIONS = 0x01.toByte()
        private const val STOP_SEND_LIVE_OBSERVATIONS = 0x02.toByte()
        private const val CONTROL_POINT_SUCCESS = 0x80.toByte()
        private const val CONTROL_POINT_SERVER_BUSY = 0x81.toByte()
        private const val CONTROL_POINT_ERROR_LIVE_OBSERVATIONS = 0x82.toByte()
    }

}