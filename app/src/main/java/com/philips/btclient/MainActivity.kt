/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.philips.btclient.acom.Observation
import com.philips.btclient.acom.SampleArrayObservationValue
import com.philips.btclient.acom.SimpleNumericObservationValue
import com.philips.btclient.fhir.FhirActivity
import com.philips.btclient.fhir.FhirUploader
import com.philips.btclient.ghs.GenericHealthSensorHandlerListener
import com.philips.btclient.ghs.GenericHealthSensorServiceHandler
import com.philips.btclient.util.timestampAsDate
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), BluetoothHandlerListener, GenericHealthSensorHandlerListener {

    var foundPeripheralsList: ListView? = null
    var foundPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    var connectedPeripheralsList: ListView? = null
    var connectedPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    var bluetoothHandler: BluetoothHandler? = null

    private val REQUEST_ENABLE_BT = 1
    private val ACCESS_LOCATION_REQUEST = 2

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        setContentView(R.layout.activity_main)

        setupFoundPeripheralsList()
        setupConnectedPeripheralsList()

        registerReceiver(
            locationServiceStateReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )
    }

    private fun setupFoundPeripheralsList() {
        foundPeripheralArrayAdapter = PeripheralArrayAdapter(
            this,
            android.R.layout.simple_list_item_1)

        foundPeripheralsList = findViewById(R.id.foundPeripheralList) as ListView
        foundPeripheralsList?.let {
            it.adapter = foundPeripheralArrayAdapter
            it.setOnItemClickListener { adapterView, view, position, l ->
                val peripheral = foundPeripheralArrayAdapter!!.getItem(position)
                peripheral?.let {
                    BluetoothHandler.getInstance(applicationContext).connect(it)
                    // Stop scanning on connect as assume we're going to use the just connected peripheral
                    setScanning(false)
                    Toast.makeText(applicationContext, "Connecting ${it.name}...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupConnectedPeripheralsList() {
        connectedPeripheralArrayAdapter = PeripheralArrayAdapter(
            this,
            android.R.layout.simple_list_item_1)

        connectedPeripheralsList = findViewById(R.id.connectedPeripheralList) as ListView
        connectedPeripheralsList?.let {
            it.adapter = connectedPeripheralArrayAdapter
            it.setOnItemClickListener { adapterView, view, position, l ->
                val peripheral = connectedPeripheralArrayAdapter?.getItem(position)
                peripheral?.let { showPeripheralInfo(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        refreshPerpheralList()
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
//            checkPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locationServiceStateReceiver)
        bluetoothHandler?.removeListener(this)
    }

    private fun refreshPerpheralList() {
        connectedPeripheralArrayAdapter?.clear()
        connectedPeripheralArrayAdapter?.addAll(BluetoothHandler.getInstance(this).getConnectedPeripherals())
    }

    private val locationServiceStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == LocationManager.MODE_CHANGED_ACTION) {
                val isEnabled = areLocationServicesEnabled()
                Timber.i("Location service state changed to: %s", if (isEnabled) "on" else "off")
                checkPermissions()
            }
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return bluetoothAdapter.isEnabled
    }

    private fun initBluetoothHandler() {
        val ghsServiceHandler = GenericHealthSensorServiceHandler()
        ghsServiceHandler.addListener(this)
        bluetoothHandler = BluetoothHandler.getInstance(applicationContext)
        bluetoothHandler?.let {
            it.addServiceHander(ghsServiceHandler)
            it.addListener(this)
            setScanning(false)
        }
    }

    private fun checkPermissions() {
        val missingPermissions = getMissingPermissions(getRequiredPermissions())
        if (missingPermissions.size > 0) {
            requestPermissions(missingPermissions, ACCESS_LOCATION_REQUEST)
        } else {
            permissionsGranted()
        }
    }

    private fun getMissingPermissions(requiredPermissions: Array<String>): Array<String> {
        val missingPermissions: MutableList<String> = ArrayList()
        for (requiredPermission in requiredPermissions) {
            if (applicationContext.checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(requiredPermission)
            }
        }
        return missingPermissions.toTypedArray()
    }

    private fun getRequiredPermissions(): Array<String> {
        val targetSdkVersion = applicationInfo.targetSdkVersion
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) else arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun permissionsGranted() {
        // Check if Location services are on because they are required to make scanning work
        if (checkLocationServices()) {
            initBluetoothHandler()
        }
    }

    private fun areLocationServicesEnabled(): Boolean {
        val locationManager =
            applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    private fun checkLocationServices(): Boolean {
        return if (!areLocationServicesEnabled()) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Location services are not enabled")
                .setMessage("Scanning for Bluetooth peripherals requires locations services to be enabled.") // Want to enable?
                .setPositiveButton("Enable") { dialogInterface, i ->
                    dialogInterface.cancel()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel") { dialog, which -> // if this button is clicked, just close
                    // the dialog box and do nothing
                    dialog.cancel()
                }
                .create()
                .show()
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if all permission were granted
        var allGranted = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }
        if (allGranted) {
            permissionsGranted()
        } else {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Location permission is required for scanning Bluetooth peripherals")
                .setMessage("Please grant permissions")
                .setPositiveButton("Retry") { dialogInterface, i ->
                    dialogInterface.cancel()
                    checkPermissions()
                }
                .create()
                .show()
        }
    }

    /*
     * BluetoothHandlerListener interface methods
     */

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral) {
        if (!(connectedPeripheralArrayAdapter?.includes(peripheral) ?: true)) {
            foundPeripheralArrayAdapter?.add(peripheral)
        }
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        foundPeripheralArrayAdapter?.remove(peripheral)
        connectedPeripheralArrayAdapter?.add(peripheral)
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        connectedPeripheralArrayAdapter?.remove(peripheral)
    }

    // GenericHealthSensorHandlerListener interface methods

    override fun onReceivedObservations(deviceAddress: String, observations: List<Observation>) {
        Timber.i("Received ${observations.size} observations from device address $deviceAddress")
        observations.forEach {
            Timber.i(it.toString())
            updateObservationText(it)
            ObservationLog.log(it)
            postObservation(it)
        }
    }

    private fun updateObservationText(observation: Observation) {
        when (observation.type) {
            ObservationType.MDC_TEMP_BODY -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(R.id.tempObservation).text = "Temp: ${floatValue} deg ${observation.timestamp}"
            }
            ObservationType.MDC_ECG_CARD_BEAT_RATE -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(R.id.hrObservation).text = "HR: ${floatValue} bpm ${observation.timestamp}"
            }
            ObservationType.MDC_SPO2_OXYGENATION_RATIO -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(R.id.spo2Observation).text = "HR: ${floatValue}% ${observation.timestamp}"
            }
            ObservationType.MDC_PPG_TIME_PD_PP -> {
                val sampleArray = (observation.value as SampleArrayObservationValue).samples
                findViewById<TextView>(R.id.ppgObservationTitle).text = "PPG Waveform ${observation.timestamp}"
                findViewById<WaveformView>(R.id.ppgObservation).setWaveform(sampleArray)
            }
            else -> findViewById<TextView>(R.id.ppgObservation).text = "${observation.type} ${observation.timestampAsDate()}"
        }
    }

    private fun postObservation(observation: Observation) {
        executor.execute {
            val result = FhirUploader.postObservation(observation)
            handler.post {
                ObservationLog.log(if (result.isSuccessful) "(Posted ${observation.type})" else "(POST error: ${result.code})")
            }
        }
    }

    // Button handling and support

    @Suppress("UNUSED_PARAMETER")
    fun toggleScanning(view: View) {
        bluetoothHandler?.let { setScanning(!it.isScanning()) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun showObservationLog(view: View) {
        startActivity(Intent(this, ObservationLogActivity::class.java))
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

    @Suppress("UNUSED_PARAMETER")
    fun openFhirSettings(view: View) {
        startActivity(Intent(this, FhirActivity::class.java))
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

    // Private methods

    private fun setScanning(enabled: Boolean) {
        foundPeripheralArrayAdapter?.clear()
        if (enabled) bluetoothHandler?.startScanning() else bluetoothHandler?.stopScanning()
        findViewById<TextView>(R.id.foundPeripheralLabel).setText(if (enabled) R.string.found_devices_scanning else R.string.found_devices_not_scanning)
        findViewById<Button>(R.id.scanButton).setText(if (enabled) R.string.stop_scanning else R.string.start_scanning)
    }

    private fun showPeripheralInfo(peripheral: BluetoothPeripheral) {
        val intent = Intent(this, PeripheralInfoActivity::class.java).apply {
            putExtra("DEVICE_ADDRESS", peripheral.address)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

}
