/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.Manifest
import android.R.layout.simple_list_item_1
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
import com.philips.bleclient.acom.Observation
import com.philips.bleclient.acom.SampleArrayObservationValue
import com.philips.bleclient.acom.SimpleNumericObservationValue
import com.philips.bleclient.fhir.FhirUploader
import com.philips.bleclient.service.ghs.GenericHealthSensorHandlerListener
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.util.timestampAsDate
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.philips.bleclient.*

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : AppCompatActivity(), ServiceHandlerManagerListener,
    GenericHealthSensorHandlerListener {

    var foundPeripheralsList: ListView? = null
    var foundPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    var connectedPeripheralsList: ListView? = null
    var connectedPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    private var ghsServiceHandler: GenericHealthSensorServiceHandler? = null
    private var serviceHandlerManager: ServiceHandlerManager? = null

    private val ACCESS_LOCATION_REQUEST = 2

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private val enableBluetoothRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            // Bluetooth has been enabled
            checkPermissions()
        } else {
            // Bluetooth has not been enabled, try again
            askToEnableBluetooth()
        }
    }

    private val isBluetoothEnabled: Boolean
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            return bluetoothAdapter.isEnabled
        }

    private fun askToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothRequest.launch(enableBtIntent)
    }

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
            simple_list_item_1
        )

        foundPeripheralsList =
            findViewById(R.id.foundPeripheralList) as ListView
        foundPeripheralsList?.let {
            it.adapter = foundPeripheralArrayAdapter
            it.setOnItemClickListener { adapterView, view, position, l ->
                val peripheral = foundPeripheralArrayAdapter!!.getItem(position)
                peripheral?.let {
                    ServiceHandlerManager.getInstance(applicationContext).connect(it)
                    // Stop scanning on connect as assume we're going to use the just connected peripheral
                    setScanning(false)
                    Toast.makeText(
                        applicationContext,
                        "Connecting ${it.name}...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupConnectedPeripheralsList() {
        connectedPeripheralArrayAdapter = PeripheralArrayAdapter(
            this,
            simple_list_item_1
        )

        connectedPeripheralsList =
            findViewById(R.id.connectedPeripheralList) as ListView
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
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if (!isBluetoothEnabled) {
                askToEnableBluetooth()
            } else {
                refreshPerpheralList()
                checkPermissions()
            }
        } else {
            Timber.e("This device has no Bluetooth hardware")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locationServiceStateReceiver)
        serviceHandlerManager?.removeListener(this)
        ghsServiceHandler?.removeListener(this)
    }

    private fun refreshPerpheralList() {
        connectedPeripheralArrayAdapter?.clear()
        connectedPeripheralArrayAdapter?.addAll(
            ServiceHandlerManager.getInstance(this).getConnectedPeripherals()
        )
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

    private fun initBluetoothHandler() {
        ghsServiceHandler = GenericHealthSensorServiceHandler()
        ghsServiceHandler!!.addListener(this)
        serviceHandlerManager = ServiceHandlerManager.getInstance(applicationContext)
        serviceHandlerManager?.let {
            it.addServiceHandler(ghsServiceHandler!!)
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
     * ServiceHandlerManagerListener interface methods
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

    /*
     * GenericHealthSensorHandlerListener interface methods
     */

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onReceivedObservations(deviceAddress: String, observations: List<Observation>) {
        Timber.i("Received ${observations.size} observations from device address $deviceAddress")
        Handler.createAsync(Looper.myLooper()!!).post {
            observations.forEach {
                Timber.i(it.toString())
                updateObservationText(it)
                ObservationLog.log(it)
                if (FhirUploader.postObservationsToServer) postObservation(it)
            }
        }
    }

    private fun updateObservationText(observation: Observation) {
        when (observation.type) {
            ObservationType.MDC_TEMP_BODY -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(com.philips.bleclient.R.id.tempObservation).text =
                    "Temp: ${floatValue} deg ${observation.timestamp}"
            }
            ObservationType.MDC_ECG_CARD_BEAT_RATE -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(com.philips.bleclient.R.id.hrObservation).text =
                    "HR: ${floatValue} bpm ${observation.timestamp}"
            }
            ObservationType.MDC_SPO2_OXYGENATION_RATIO -> {
                val floatValue = (observation.value as SimpleNumericObservationValue).value
                findViewById<TextView>(com.philips.bleclient.R.id.spo2Observation).text =
                    "HR: ${floatValue}% ${observation.timestamp}"
            }
            ObservationType.MDC_PPG_TIME_PD_PP -> {
                val sampleArray = (observation.value as SampleArrayObservationValue).samples
                findViewById<TextView>(com.philips.bleclient.R.id.ppgObservationTitle).text =
                    "PPG Waveform ${observation.timestamp}"
                findViewById<WaveformView>(com.philips.bleclient.R.id.ppgObservation).setWaveform(
                    sampleArray
                )
            }
            else -> findViewById<TextView>(com.philips.bleclient.R.id.ppgObservation).text =
                "${observation.type} ${observation.timestampAsDate()}"
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

    /*
     * Button handling and support
     */

    @Suppress("UNUSED_PARAMETER")
    fun toggleScanning(view: View) {
        serviceHandlerManager?.let { setScanning(!it.isScanning()) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun showObservationLog(view: View) {
        startActivity(Intent(this, ObservationLogActivity::class.java))
        overridePendingTransition(
            R.anim.slide_from_right,
            R.anim.slide_to_left
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun openFhirSettings(view: View) {
        startActivity(Intent(this, FhirActivity::class.java))
        overridePendingTransition(
            R.anim.slide_from_right,
            R.anim.slide_to_left
        )
    }

    // Private methods

    private fun setScanning(enabled: Boolean) {
        foundPeripheralArrayAdapter?.clear()
        if (enabled) serviceHandlerManager?.startScanning() else serviceHandlerManager?.stopScanning()
        findViewById<TextView>(R.id.foundPeripheralLabel).setText(if (enabled) com.philips.bleclient.R.string.found_devices_scanning else com.philips.bleclient.R.string.found_devices_not_scanning)
        findViewById<Button>(R.id.scanButton).setText(if (enabled) com.philips.bleclient.R.string.stop_scanning else com.philips.bleclient.R.string.start_scanning)
    }

    private fun showPeripheralInfo(peripheral: BluetoothPeripheral) {
        val intent = Intent(this, PeripheralInfoActivity::class.java).apply {
            putExtra("DEVICE_ADDRESS", peripheral.address)
        }
        startActivity(intent)
        overridePendingTransition(
            R.anim.slide_from_right,
            R.anim.slide_to_left
        )
    }

}
