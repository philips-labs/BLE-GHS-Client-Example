/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.sts.SimpleTimeServiceHandlerListener
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

class PeripheralInfoActivity : AppCompatActivity(), SimpleTimeServiceHandlerListener {
    private var peripheral: BluetoothPeripheral? = null
    private var ghsServiceHandler = ServiceHandlerManager.getInstance(this).getGhsServiceHandler()
    private var stsServiceHandler = ServiceHandlerManager.getInstance(this).getStsServiceHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        var title = "No peripheral '$deviceAddress' found"
        deviceAddress?.let { addr ->
            peripheral = ServiceHandlerManager.getInstance(this).getConnectedPeripheral(addr)
            peripheral?.let {
                setupPeripheral(it)
                title = "${it.name} information"
            }
        }

        stsServiceHandler?.addListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                goBack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    private fun goBack() {
        finish()
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
    }

    private fun setupPeripheral(periph: BluetoothPeripheral) {
        findViewById<TextView>(R.id.peripheralMacAddress).text = "MAC address: ${periph.address}"
    }

    @Suppress("UNUSED_PARAMETER")
    fun disconnectPeripheral(view: View) {
        peripheral?.cancelConnection()
        goBack()
    }

    @Suppress("UNUSED_PARAMETER")
    fun enableLiveObservations(view: View) {
        ghsServiceHandler?.let { peripheral?.let { p -> it.enableLiveObservations(p) } }
    }

    @Suppress("UNUSED_PARAMETER")
    fun disableLiveObservations(view: View) {
        ghsServiceHandler?.let { peripheral?.let { p -> it.disableLiveObservations(p) } }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getStsBytes(view: View) {
        peripheral?.let { stsServiceHandler?.getSTSBytes(it) }
    }

    /*
     * SimpleTimeServiceHandlerListener methods
     */
    override fun onReceivedStsBytes(deviceAddress: String, bytes: ByteArray) {
        Timber.i("STS Bytes: ${bytes.asFormattedHexString() ?: "NULL"}")
    }

}

fun ServiceHandlerManager.getGhsServiceHandler(): GenericHealthSensorServiceHandler? {
    return serviceHandlerForUUID(GenericHealthSensorServiceHandler.SERVICE_UUID)?.let {it as GenericHealthSensorServiceHandler}
}
