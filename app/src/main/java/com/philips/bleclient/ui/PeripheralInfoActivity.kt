/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.extensions.BitMask
import com.philips.bleclient.extensions.TimestampFlags
import com.philips.bleclient.extensions.hasFlag
import com.philips.bleclient.extensions.parseSTSDate
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.sts.SimpleTimeServiceHandlerListener
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber


class PeripheralInfoActivity : AppCompatActivity(), SimpleTimeServiceHandlerListener, AdapterView.OnItemSelectedListener {
    private var peripheral: BluetoothPeripheral? = null
    private var ghsServiceHandler = ServiceHandlerManager.getInstance(this).getGhsServiceHandler()
    private var stsServiceHandler = ServiceHandlerManager.getInstance(this).getStsServiceHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.philips.bleclient.R.layout.activity_device_info)
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

        val spinner = findViewById<View>(R.id.stsDateType) as Spinner
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, arrayOf("Local Time", "UTC only", "UTC + Offset")
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(adapter)
        spinner.setOnItemSelectedListener(this)

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
        overridePendingTransition(com.philips.bleclient.R.anim.slide_from_left, com.philips.bleclient.R.anim.slide_to_right)
    }

    private fun setupPeripheral(periph: BluetoothPeripheral) {
        findViewById<TextView>(com.philips.bleclient.R.id.peripheralMacAddress).text = "MAC address: ${periph.address}"
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

    @Suppress("UNUSED_PARAMETER")
    fun setStsClockBytes(view: View) {
        peripheral?.let { stsServiceHandler?.setSTSBytes(it) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun resetTickCounter(view: View) {
        peripheral?.let { stsServiceHandler?.resetTickCounter(it) }
    }

    /*
     * SimpleTimeServiceHandlerListener methods
     */
    override fun onReceivedStsBytes(deviceAddress: String, bytes: ByteArray) {
        Timber.i("STS Bytes: ${bytes.asFormattedHexString()}")
        if (BitMask(bytes.first().toLong()).hasFlag(TimestampFlags.isTickCounter)) {
            Timber.i("STS Date (Ticks so should be null): ${bytes.parseSTSDate()}")
        } else {
            val stsDate = "Read STS Characteristic Date: ${bytes.parseSTSDate()}"
            Timber.i(stsDate)
            ObservationLog.log(stsDate)
        }
    }

    /*
     * Spinner methods
     */

    override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
        when (position) {
            0 -> {}
            1 -> {}
            2 -> {}
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }

}

fun ServiceHandlerManager.getGhsServiceHandler(): GenericHealthSensorServiceHandler? {
    return serviceHandlerForUUID(GenericHealthSensorServiceHandler.SERVICE_UUID)?.let {it as GenericHealthSensorServiceHandler}
}
