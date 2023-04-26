package com.philips.bleclient.service.ghs

import com.philips.bleclient.observations.Observation
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

class GhsObservationHandler(val service: GenericHealthSensorServiceHandler, val isStored: Boolean = false): GenericHealthSensorPacketListener {

    private val packetHandler = GenericHealthSensorPacketHandler(this, isStored)

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        //Timber.i("Received Observation Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        packetHandler.receiveBytes(peripheral.address, value)
    }

    /*
     * GenericHealthSensorPacketListener methods (called when all segments have been received and
     * have a full ACOM object bytes or an error in the received BLE segments
     */

    override fun onReceivedMessageBytes(deviceAddress: String, byteArray: ByteArray) {
        Timber.i("Received Message of ${byteArray.size} bytes")
        val bytesForObs: ByteArray = (if(isStored) byteArray.takeLast(byteArray.size-4).toByteArray() else byteArray)
        Observation.fromBytes(bytesForObs)?.let {
            if (isStored) {
                val parser = BluetoothBytesParser(byteArray)
                val recordnumber = parser.uInt32
                Timber.i("received recordnumber: ${recordnumber}")
                service.receivedStoredObservation(deviceAddress, it)
            } else {
                service.receivedObservation(deviceAddress, it)
            }
        }
    }

    override fun onReceiveBytesOverflow(deviceAddress: String, byteArray: ByteArray) {
        Timber.e("Error BYTES OVERFLOW: $deviceAddress bytes: <${byteArray.asFormattedHexString()}>")
    }

}