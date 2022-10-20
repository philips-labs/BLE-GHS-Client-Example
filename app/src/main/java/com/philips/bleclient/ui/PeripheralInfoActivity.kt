/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.extensions.*
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.sts.SimpleTimeServiceHandlerListener
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BondState
import timber.log.Timber


class PeripheralInfoActivity : AppCompatActivity(), SimpleTimeServiceHandlerListener {
    private var peripheral: BluetoothPeripheral? = null
    private var ghsServiceHandler = ServiceHandlerManager.getInstance(this).getGhsServiceHandler()
    private var stsServiceHandler = ServiceHandlerManager.getInstance(this).getStsServiceHandler()

    private enum class TimeType {
        LOCAL_TIME,
        LOCAL_TIME_WITH_OFFSET,
        UTC_ONLY,
        UTC_WITH_OFFSET,
        TICK_COUNTER
    }

    private var writeTimeType = TimeType.TICK_COUNTER

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

        setupTimeValueType()
        setupTimeSource()

    }

    private fun setupTimeValueType() {
        setupSpinner(R.id.stsDateType, arrayOf("Local Time", "UTC only", "UTC + Offset", "Ticks", "Local with DST"), timeValueTypeListener())
    }

    private fun setupTimeSource() {
        setupSpinner(R.id.stsTimeSource, arrayOf("Manual", "Cellular Network", "GPS"), timeSourceListener())
    }

    private fun timeValueTypeListener(): OnItemSelectedListener {
        return object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        writeTimeType = TimeType.LOCAL_TIME
                        Timber.i("Set Local TimestampFlags")
                        TimestampFlags.setLocalFlags()
                    }
                    1 -> {
                        writeTimeType = TimeType.UTC_ONLY
                        Timber.i("Set UTC_ONLY TimestampFlags")
                        TimestampFlags.setUtcOnlyFlags()
                    }
                    2 -> {
                        writeTimeType = TimeType.UTC_WITH_OFFSET
                        Timber.i("Set UTC_WITH_OFFSET TimestampFlags")
                        TimestampFlags.setUtcWithOffsetFlags()
                    }
                    3 -> {
                        writeTimeType = TimeType.TICK_COUNTER
                        Timber.i("Set TICK_COUNTER TimestampFlags")
                        TimestampFlags.setTickCounterFlags()
                    }
                    4 -> {
                        writeTimeType = TimeType.LOCAL_TIME_WITH_OFFSET
                        Timber.i("Set LOCAL_TIME_WITH_OFFSET TimestampFlags")
                        TimestampFlags.setLocalWithOffsetFlags()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun timeSourceListener(): OnItemSelectedListener {
        return object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                Timesource.currentSource = when (position) {
                    0 -> Timesource.Manual
                    1 -> Timesource.CellularNetwork
                    2 -> Timesource.GPS
                    else -> Timesource.Unknown
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSpinner(resourceId: Int, items: Array<String>, listener: OnItemSelectedListener) {
        val spinner = findViewById<View>(resourceId) as Spinner
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = listener
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
        overridePendingTransition(R.anim.slide_from_left, com.philips.bleclient.R.anim.slide_to_right)
    }

    private fun setupPeripheral(periph: BluetoothPeripheral) {
        findViewById<TextView>(R.id.peripheralMacAddress).text = "MAC address: ${periph.address}"
    }

    @Suppress("UNUSED_PARAMETER")
    fun disconnectPeripheral(view: View) {
//        peripheral?.let {
//            if(it.isBonded()) {
//                ServiceHandlerManager.getInstance(applicationContext).unbond(it)
//            }
//            it.cancelConnection()
//        }
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
    fun writeObservationSchedule(view: View) {
        ghsServiceHandler?.let {
            peripheral?.let {
                p -> it.writeObservationSchedule(p, ObservationType.MDC_PULS_OXIM_SAT_O2, 1f, 1f)
            }
        }
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

}

fun ServiceHandlerManager.getGhsServiceHandler(): GenericHealthSensorServiceHandler? {
    return serviceHandlerForUUID(GenericHealthSensorServiceHandler.SERVICE_UUID)?.let {it as GenericHealthSensorServiceHandler}
}

fun BluetoothPeripheral.isBonded(): Boolean { return !(bondState == BondState.NONE) }
