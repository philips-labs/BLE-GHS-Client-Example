package com.philips.btclient

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.philips.btclient.acom.Observation
import com.philips.btclient.ghs.GenericHealthSensorHandlerListener
import com.philips.btclient.ghs.GenericHealthSensorServiceHandler
import com.philips.btserver.generichealthservice.ObservationType
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity(), BluetoothHandlerListener, GenericHealthSensorHandlerListener {

    private lateinit var measurementValue: TextView
    private val REQUEST_ENABLE_BT = 1
    private val ACCESS_LOCATION_REQUEST = 2

    var foundPeripheralsList: ListView? = null
    var foundPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    var connectedPeripheralsList: ListView? = null
    var connectedPeripheralArrayAdapter: PeripheralArrayAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        setContentView(R.layout.activity_main)

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
                    Toast.makeText(applicationContext, "Connecting ${it.name}...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        connectedPeripheralArrayAdapter = PeripheralArrayAdapter(
            this,
            android.R.layout.simple_list_item_1)

        connectedPeripheralsList = findViewById(R.id.connectedPeripheralList) as ListView
        connectedPeripheralsList?.let {
            it.adapter = connectedPeripheralArrayAdapter
            it.setOnItemClickListener { adapterView, view, position, l ->
                val peripheral = connectedPeripheralArrayAdapter!!.getItem(position)
                peripheral?.let {
                    Toast.makeText(applicationContext, "Clicked ${it.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        registerReceiver(
            locationServiceStateReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
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
        BluetoothHandler.getInstance(applicationContext).addServiceHander(ghsServiceHandler)
        BluetoothHandler.getInstance(applicationContext).addListener(this)
        BluetoothHandler.getInstance(applicationContext).startScanning()
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
        connectedPeripheralArrayAdapter?.let {
            for (i in 0 until it.getCount()) {
                if (it.getItem(i) == peripheral) {
                    return
                }
            }
        }
        foundPeripheralArrayAdapter?.let {
            for (i in 0 until it.getCount()) {
                if (it.getItem(i) == peripheral) return
            }
            foundPeripheralArrayAdapter?.add(peripheral)
        }
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        foundPeripheralArrayAdapter?.remove(peripheral)
        connectedPeripheralArrayAdapter?.let {
            for (i in 0 until it.getCount()) {
                if (it.getItem(i) == peripheral) return
            }
            it.add(peripheral)
        }
    }

    // GenericHealthSensorHandlerListener interface methods
    override fun onReceivedObservations(deviceAddress: String, observations: List<Observation>) {
        Timber.i("Received ${observations.size} observations from device address $deviceAddress")
        observations.forEach {
            Timber.i(it.toString())
        }
    }

}