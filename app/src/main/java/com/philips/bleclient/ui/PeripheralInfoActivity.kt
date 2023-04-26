/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.bluetooth.BluetoothGattCharacteristic
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
import com.philips.bleclient.service.dis.DisInfoMap
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.rcs.RCSServiceHandler
import com.philips.bleclient.service.ets.ElapsedTimeServiceHandlerListener
import com.philips.bleclient.service.user.UserDataServiceHandler
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BondState
import timber.log.Timber
import kotlin.random.Random


class PeripheralInfoActivity : AppCompatActivity(), ElapsedTimeServiceHandlerListener {
    private var peripheral: BluetoothPeripheral? = null
    private var ghsServiceHandler = ServiceHandlerManager.getInstance(this).getGhsServiceHandler()
    private var stsServiceHandler = ServiceHandlerManager.getInstance(this).getStsServiceHandler()
    private var rcsServiceHandler = ServiceHandlerManager.getInstance(this).getRcsServiceHandler()
    private var useIndicationsForLiveData = false

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
                Timber.i("Device Info Screen: $deviceAddress\nSupported Observations:${GHSDeviceInfoMap.getSupportedObservationTypes(deviceAddress)}")
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        stsServiceHandler?.addListener(this)

        setupTimeValueType()
        setupTimeSource()

    }

    override fun onResume() {
        getPeripheral()?.let {updateDeviceInfoMap(it)}
        super.onResume()
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
        findViewById<TextView>(R.id.peripheralMacAddress).text = "Device address: ${periph.address}"
        findViewById<TextView>(R.id.supportedObservationTypes).text = "Supported Observations: ${GHSDeviceInfoMap.getSupportedObservationTypes(periph.address)}\nDevice Specializations: ${GHSDeviceInfoMap.getSupportedSpecializations(periph.address)}"
        updateDeviceInfoMap(periph)
    }

    private fun updateDeviceInfoMap(periph: BluetoothPeripheral) {
        findViewById<TextView>(R.id.disInfoView).text = DisInfoMap.getInfo(periph.address)
    }

    private fun getPeripheral(): BluetoothPeripheral? {
        return UserDataServiceHandler.instance?.getCurrentCentrals()?.first()
    }

    @Suppress("UNUSED_PARAMETER")
    fun disconnectPeripheral(view: View) {
        peripheral?.let {
            if(it.isBonded()) {
                ServiceHandlerManager.getInstance(applicationContext).unbond(it)
            }
            it.cancelConnection()
        }
        peripheral?.cancelConnection()
        goBack()
    }

    @Suppress("UNUSED_PARAMETER")
    fun enableDisconnect(view: View) {
        peripheral?.let {
            rcsServiceHandler?.enableDisconnect((it))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun enableLiveObservations(view: View) {
        ghsServiceHandler?.let { peripheral?.let { p -> it.enableLiveObservations(p) } }
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendInvalidCommand(view: View) {
        ghsServiceHandler?.let { peripheral?.let { p -> it.sendInvalidCommand(p) } }
    }

    @Suppress("UNUSED_PARAMETER")
    fun disableLiveObservations(view: View) {
        ghsServiceHandler?.let { peripheral?.let { p -> it.disableLiveObservations(p) } }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getStsBytes(view: View) {
        peripheral?.let { stsServiceHandler?.getETSBytes(it) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setStsClockBytes(view: View) {
        peripheral?.let { stsServiceHandler?.setETSBytes(it) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun writeObservationSchedule(view: View) {
        ghsServiceHandler?.let {
            peripheral?.let {
                p -> it.writeObservationSchedule(p, Random.nextInt(10)+1f, Random.nextInt(10)*2f + 1)
            }
        }
    }

    fun randomMeasurementPeriod() : Float {
        return Random.nextFloat() * 10
    }

    @Suppress("UNUSED_PARAMETER")
    fun readObservationSchedule(view: View) {
        ghsServiceHandler?.let {
            peripheral?.let { p -> it.debugObservationScheduleDescriptors(p) }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleIndicationsLive(view: View) {
        useIndicationsForLiveData = (view as Switch).isChecked

        val notifyProperty = if (useIndicationsForLiveData) {
            ObservationLog.log("Using Indications for Live Data")
            Timber.i("Using Indications for Live Data")
            BluetoothGattCharacteristic.PROPERTY_INDICATE
        } else {
            ObservationLog.log("Using Notificiations for Live Data")
            Timber.i("Using Notificiations for Data")
            BluetoothGattCharacteristic.PROPERTY_NOTIFY
        }
        ghsServiceHandler?.useIndicationsForLive(notifyProperty)

    }

    private val logView get() = findViewById<TextView>(R.id.deviceInfoLog)

    /*
     * ElapsedTimeServiceHandlerListener methods
     */
    override fun onReceivedEtsBytes(deviceAddress: String, bytes: ByteArray) {
        var logString = "Received ETS Bytes: ${bytes.asFormattedHexString()}\n"
        logString += bytes.etsDateInfoString()
        Timber.i(logString)
        logView.text = "${logView.text} + $logString\n"
    }

}

fun ServiceHandlerManager.getGhsServiceHandler(): GenericHealthSensorServiceHandler? {
    return serviceHandlerForUUID(GenericHealthSensorServiceHandler.SERVICE_UUID)?.let {it as GenericHealthSensorServiceHandler}
}

fun ServiceHandlerManager.getRcsServiceHandler(): RCSServiceHandler? {
    return serviceHandlerForUUID(RCSServiceHandler.RCS_SERVICE_UUID)?.let {it as RCSServiceHandler }
}

fun BluetoothPeripheral.isBonded(): Boolean { return !(bondState == BondState.NONE) }
