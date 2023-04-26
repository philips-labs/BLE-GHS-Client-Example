/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.Manifest
import android.R.layout.simple_list_item_1
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.fhir.FhirUploader
import com.philips.bleclient.service.ghs.GenericHealthSensorHandlerListener
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.philips.bleclient.*
import com.philips.bleclient.observations.*
import com.philips.bleclient.extensions.asDisplayString
import com.philips.bleclient.service.bas.BasServiceHandler
import com.philips.bleclient.service.dis.DisServiceHandler
import com.philips.bleclient.service.ghs.DeviceSpecialization
import com.philips.bleclient.service.rcs.RCSServiceHandler
import com.philips.bleclient.service.ets.ElapsedTimeServiceHandler
import com.philips.bleclient.service.user.UserDataServiceHandler

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : AppCompatActivity(), ServiceHandlerManagerListener,
    GenericHealthSensorHandlerListener {

    var foundPeripheralsList: ListView? = null
    var foundPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    var connectedPeripheralsList: ListView? = null
    var connectedPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    private var ghsServiceHandler: GenericHealthSensorServiceHandler? = null
    private var etsServiceHandler: ElapsedTimeServiceHandler? = null
    private var udsServiceHandler: UserDataServiceHandler? = null
    private var disServiceHandler: DisServiceHandler? = null
    private var basServiceHandler: BasServiceHandler? = null
    private var rcsServiceHandler: RCSServiceHandler? = null
    private var serviceHandlerManager: ServiceHandlerManager? = null

    private val ACCESS_LOCATION_REQUEST = 2

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var bondPeripherals = false

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

//    override fun onConnected(peripheral: BluetoothPeripheral) {
//        ghsServiceHandler?.onConnectedPeripheral(peripheral)
//        disServiceHandler?.onConnectedPeripheral(peripheral)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(AppLogTree())
        setContentView(R.layout.activity_main)

        setupFoundPeripheralsList()
        setupConnectedPeripheralsList()

        registerReceiver(
            locationServiceStateReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )

        registerReceiver(
            bluetoothBondingReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
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
                    if (bondPeripherals)
                        ServiceHandlerManager.getInstance(applicationContext).bond(it)
                    else
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

//        if (BluetoothAdapter.getDefaultAdapter() != null) {
        if (getBluetoothAdapter() != null) {
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
        ObservationSyncer.disconnect()
    }

    private fun getBluetoothAdapter(): BluetoothAdapter {
        return (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
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

    private val bluetoothBondingReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.action)){
                val extra: Parcelable? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                extra?.let {
                    val mDevice = it as BluetoothDevice
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                            Timber.i("Device bonded")
                        }
                    }
                }
            }
        }
    }

    private fun initBluetoothHandler() {
        serviceHandlerManager = ServiceHandlerManager.getInstance(applicationContext)
        initGHSServiceHandler()
        initETSServiceHandler()
        initUDSServiceHandler()
        initDISServiceHandler()
        initRCSServiceHandler()
        initBASSServiceHandler()
        serviceHandlerManager?.let {
            //it.addServiceHandler(ghsServiceHandler!!)
            it.addListener(this)
            setScanning(false)
        }
        ObservationSyncer.connect()
    }

    private fun initDISServiceHandler() {
        if (disServiceHandler == null) {
            disServiceHandler = DisServiceHandler()
            //disServiceHandler!!.addListener(this)
            serviceHandlerManager?.addServiceHandler(disServiceHandler!!)
        }
    }

    private fun initBASSServiceHandler() {
        if (basServiceHandler == null) {
            basServiceHandler = BasServiceHandler()
            //disServiceHandler!!.addListener(this)
            serviceHandlerManager?.addServiceHandler(basServiceHandler!!)
        }
    }

    private fun initGHSServiceHandler() {
        if (ghsServiceHandler == null) {
            ghsServiceHandler = GenericHealthSensorServiceHandler()
            ghsServiceHandler!!.addListener(this)
            ghsServiceHandler!!.addListener(GHSDeviceInfoMap)
            serviceHandlerManager?.addServiceHandler(ghsServiceHandler!!)
        }
    }

    private fun initETSServiceHandler() {
        if (etsServiceHandler == null) {
            etsServiceHandler = ElapsedTimeServiceHandler()
            serviceHandlerManager?.addServiceHandler(etsServiceHandler!!)
        }
    }

    private fun initRCSServiceHandler() {
        if (rcsServiceHandler == null) {
            rcsServiceHandler = RCSServiceHandler()
            serviceHandlerManager?.addServiceHandler(rcsServiceHandler!!)
        }
    }


    private fun initUDSServiceHandler() {
        if (udsServiceHandler == null) {
            udsServiceHandler = UserDataServiceHandler()
            serviceHandlerManager?.addServiceHandler(udsServiceHandler!!)
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN,  Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else  {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun permissionsGranted() {
        // Check if Location services are on because they are required to make scanning work
        checkLocationServices()
        initBluetoothHandler()
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

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        if (!(connectedPeripheralArrayAdapter?.includes(peripheral) ?: true)) {
            foundPeripheralArrayAdapter?.add(peripheral)
        }
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        ObservationLog.log("Connected: ${peripheral.name}")
        foundPeripheralArrayAdapter?.remove(peripheral)
        connectedPeripheralArrayAdapter?.add(peripheral)
        //        ghsServiceHandler?.onConnectedPeripheral(peripheral)
//        disServiceHandler?.onConnectedPeripheral(peripheral)

    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        ObservationLog.log("Disconnected: ${peripheral.name}")
        connectedPeripheralArrayAdapter?.remove(peripheral)
    }

    /*
     * GenericHealthSensorHandlerListener interface methods
     */

    override fun onReceivedObservations(deviceAddress: String, observations: List<Observation>) {
        Timber.i("Received ${observations.size} observations from device address $deviceAddress")
        Handler(Looper.myLooper()!!).post {
            observations.forEach {
                // Assign so smart casting works
                val observationValue = it.value
                if (observationValue is BundledObservationValue) {
                    observationValue.observations.forEach { processReceviedObservation(it) }
                } else {
                    processReceviedObservation(it)
                }
            }
        }
    }

    override fun onSupportedObservationTypes(deviceAddress: String, observationTypes: List<ObservationType>) {
        ObservationLog.log("Device: $deviceAddress\nSupported Observations:${observationTypes}")
        val allTypes = mutableListOf(
            ObservationType.MDC_TEMP_BODY,
            ObservationType.MDC_ECG_CARD_BEAT_RATE,
            ObservationType.MDC_PULS_OXIM_SAT_O2,
            ObservationType.MDC_PRESS_BLD_NONINV,
            ObservationType.MDC_PPG_TIME_PD_PP
        )
        observationTypes.forEach {
            allTypes.remove(it)
            setVisibilityForObservationType(it, true)
        }
        allTypes.forEach {
            setVisibilityForObservationType(it, false)
        }
    }

    override fun onSupportedDeviceSpecializations(
        deviceAddress: String,
        deviceSpecializations: List<DeviceSpecialization>
    ) {
        TODO("Not yet implemented")
    }

    private fun processReceviedObservation(observation: Observation) {
        Timber.i(observation.toString())
        updateObservationText(observation)
        ObservationLog.log(observation)
        if (FhirUploader.postObservationsToServer) postObservation(observation)
    }

    private fun setVisibilityForObservationType(type: ObservationType, visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        when (type) {
            ObservationType.MDC_TEMP_BODY -> {
                findViewById<TextView>(R.id.tempObservation).visibility = visibility
            }
            ObservationType.MDC_ECG_CARD_BEAT_RATE -> {
                findViewById<TextView>(R.id.hrObservation).visibility = visibility
            }
            ObservationType.MDC_PULS_OXIM_SAT_O2 -> {
                findViewById<TextView>(R.id.spo2Observation).visibility = visibility
            }
            ObservationType.MDC_PRESS_BLD_NONINV -> {
                findViewById<TextView>(R.id.bpObservation).visibility = visibility
            }
            ObservationType.MDC_PPG_TIME_PD_PP -> {
                findViewById<TextView>(R.id.ppgObservationTitle).visibility = visibility
                findViewById<WaveformView>(R.id.ppgObservation).visibility = visibility
            }
            ObservationType.UNKNOWN -> {
                ObservationLog.log("Unknown Observeration: $type")
            }
            else -> {
                ObservationLog.log("Unsupported Observeration Type: $type")
            }
        }
    }

    private fun updateObservationText(observation: Observation) {
        findViewById<TextView>(R.id.patientId).text =  "Patient Id: ${observation.patientId.toString()}"
        when (observation.type) {
            ObservationType.MDC_TEMP_BODY -> {
                val textView = findViewById<TextView>(R.id.tempObservation)
                if (observation.value is SimpleNumericObservationValue) {
                    val floatValue = (observation.value as SimpleNumericObservationValue).value
                    textView.text = "Temp: ${floatValue} deg ${observation.timestamp?.asDisplayString()}"
                } else {
                    textView.text = "Temp Value is a ${observation.value?.javaClass}"
                }
            }
            ObservationType.MDC_ECG_CARD_BEAT_RATE -> {
                val textView = findViewById<TextView>(R.id.hrObservation)
                if (observation.value is SimpleNumericObservationValue) {
                    val floatValue = (observation.value as SimpleNumericObservationValue).value
                    textView.text = "HR: ${floatValue} bpm ${observation.timestamp?.asDisplayString()}"
                } else {
                    textView.text = "HR Value is a ${observation.value?.javaClass}"
                }
            }
            ObservationType.MDC_PULS_OXIM_SAT_O2 -> {
                val textView = findViewById<TextView>(R.id.spo2Observation)
                if (observation.value is SimpleNumericObservationValue) {
                    val floatValue = (observation.value as SimpleNumericObservationValue).value
                    textView.text = "SpO2: ${floatValue}% ${observation.timestamp?.asDisplayString()}"
                } else {
                    textView.text = "SpO2 Value is a ${observation.value?.javaClass}"
                }
            }
            ObservationType.MDC_PRESS_BLD_NONINV -> {
                var valString = ""
                var seperator = ""
                val textView = findViewById<TextView>(R.id.bpObservation)
                if (observation.value is CompoundObservationValue) {
                    (observation.value as CompoundObservationValue).values.forEach {
                        valString = "$valString $seperator ${it.value}"
                        seperator = "/"
                    }
                    textView.text = "Blood pressure: $valString ${observation.timestamp?.asDisplayString()}"
                } else {
                    textView.text = "Blood pressure Value is a ${observation.value?.javaClass}"
                }
            }
            ObservationType.MDC_PPG_TIME_PD_PP -> {
                val textView = findViewById<TextView>(R.id.ppgObservationTitle)
                if (observation.value is SampleArrayObservationValue) {
                    textView.text = "PPG Waveform: ${observation.timestamp?.asDisplayString()}"
                    val samples = (observation.value as SampleArrayObservationValue).samples
                    (findViewById<WaveformView>(R.id.ppgObservation)).setWaveform(samples)
                } else {
                    textView.text = "PPG Waveform Value is a ${observation.value?.javaClass}"
                }
            }
            ObservationType.UNKNOWN -> {
                ObservationLog.log("Received Unknown Observeration: $observation")
            }
            ObservationType.MDC_DRUG_NAME_LABEL -> {
                ObservationLog.log("Received String Observeration: $observation")
            }
            else -> {
                ObservationLog.log("Received Observeration: $observation")
            }
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

    fun toggleBonding(view: View) {
        bondPeripherals = (view as Switch).isChecked
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
    fun showRacp(view: View) {
        startActivity(Intent(this, RacpActivity::class.java))
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

    @Suppress("UNUSED_PARAMETER")
    fun openUsers(view: View) {
        startActivity(Intent(this, UsersActivity::class.java))
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
